package polycube.polycard.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.PolyCard;
import polycube.polycard.data.Equipment;
import polycube.polycard.gui.EquipmentGUI;
import polycube.polycore.commands.PolyCommand;
import polycube.polycore.text.TextComponents;

public class EquipCommand extends PolyCommand {
    public EquipCommand() {
        super(
                PolyCard.MOD_ID,
                "equip",
                "Open your " + Equipment.MAX_CARDS + "-slot equipment manager; selecting another player requires the Gamemasters permission level",
                "[player]",
                PermissionLevel.ALL,
                true
        );
    }

    @Override
    protected int execute(CommandSourceStack source) {

        var player = source.getPlayer();
        if (player == null) {
            source.sendFailure(TextComponents.error("This command can only be executed by a player."));
            return 0;
        }

        EquipmentGUI.openEquipmentGUI(player);
        return 1;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommand(String name) {
        return super.getCommand(name).then(
                Commands.argument("player", EntityArgument.player())
                        .requires(src -> hasPermission(src, PermissionLevel.GAMEMASTERS))
                        .executes(cts -> {
                            var source = cts.getSource();
                            var viewer = source.getPlayer();
                            if (viewer == null) {
                                source.sendFailure(TextComponents.error("This command can only be executed by a player."));
                                return 0;
                            }

                            var targetPlayer = EntityArgument.getPlayer(cts, "player");
                            EquipmentGUI.openEquipmentGUI(viewer, targetPlayer);
                            source.sendSuccess(() -> TextComponents.success("Opening equipment manager for ")
                                    .append(TextComponents.value(targetPlayer.getName()))
                                    .append(TextComponents.muted("...")), false);
                            return 1;
                        })
        );
    }
}
