package polycube.polycard.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.Permissions;
import polycube.polycard.PolyCard;
import polycube.polycard.gui.EquipmentGUI;

public class EquipCommand extends PolyCardCommand {
    private final EquipmentGUI equipmentGUI;

    public EquipCommand(EquipmentGUI equipmentGUI) {
        super(
                "equip",
                "Open the equipment manager to equip up to 5 cards.",
                "/" + PolyCard.MOD_ID + " equip",
                PermissionLevel.ALL
        );

        this.equipmentGUI = equipmentGUI;
    }

    @Override
    protected int execute(CommandSourceStack source) {

        var player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be executed by a player."));
            return 0;
        }

        equipmentGUI.openEquipmentGUI(player);

        source.sendSuccess(() -> Component.literal("Opening equipment manager...").withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> getCommand() {
        return super.getCommand().then(
                Commands.argument("player", EntityArgument.players())
                        .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                        .executes(cts -> {
                            var source = cts.getSource();
                            var player = EntityArgument.getPlayer(cts, "player");
                            equipmentGUI.openEquipmentGUI(source.getPlayer(), player);
                            source.sendSuccess(() -> Component.literal("Opening equipment manager for " + player.getName().getString() + "...").withStyle(ChatFormatting.GOLD), false);
                            return 1;
                        })
        );
    }
}
