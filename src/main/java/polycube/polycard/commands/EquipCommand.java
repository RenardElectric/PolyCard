package polycube.polycard.commands;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;
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
}
