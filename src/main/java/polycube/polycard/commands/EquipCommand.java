package polycube.polycard.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.data.Equipment;
import polycube.polycard.gui.EquipmentGUI;

public class EquipCommand extends PolyCardCommand {
    public EquipCommand() {
        super(
                "equip",
                "Open the equipment manager to equip up to " + Equipment.MAX_CARDS + " cards",
                "[player]",
                PermissionLevel.ALL
        );
    }

    @Override
    protected int execute(CommandSourceStack source) {

        var player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be executed by a player."));
            return 0;
        }

        EquipmentGUI.openEquipmentGUI(player);
        return 1;
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> getCommand() {
        return super.getCommand().then(
                Commands.argument("player", EntityArgument.player())
                        .requires(src -> hasPermission(src, PermissionLevel.GAMEMASTERS))
                        .executes(cts -> {
                            var source = cts.getSource();
                            var viewer = source.getPlayer();
                            if (viewer == null) {
                                source.sendFailure(Component.literal("This command can only be executed by a player."));
                                return 0;
                            }

                            var targetPlayer = EntityArgument.getPlayer(cts, "player");
                            EquipmentGUI.openEquipmentGUI(viewer, targetPlayer);
                            source.sendSuccess(() -> Component.literal("Opening equipment manager for " + targetPlayer.getName().getString() + "...").withStyle(ChatFormatting.GOLD), false);
                            return 1;
                        })
        );
    }
}
