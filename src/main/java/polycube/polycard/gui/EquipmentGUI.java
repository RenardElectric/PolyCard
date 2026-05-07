package polycube.polycard.gui;

import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import polycube.polycard.manager.CardManager;
import polycube.polycard.manager.Storage;

public class EquipmentGUI {
    private final Storage storage;

    public EquipmentGUI(Storage storage) {
        this.storage = storage;
    }

    /// Opens the equipment GUI for a player - single line with 5 centered slots.
    ///
    /// @param player The player to open the GUI for.
    public void openEquipmentGUI(ServerPlayer player) {

        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x1, player, false);

        gui.setTitle(Component.literal("Equipment Slots").withStyle(ChatFormatting.GOLD));
        var container = storage.data(player).asContainer();
        for (int i = 0; i < 5; i++) {
            gui.setSlot(2 + i, new Slot(container, i, 0, 0) {
                @Override
                public boolean mayPlace(@NonNull ItemStack itemStack) {
                    player.sendSystemMessage(Component.literal("Trying to place " + itemStack.getHoverName().getString() + " in slot " + getContainerSlot()), false);
                    if (itemStack.isEmpty()) {
                        player.sendSystemMessage(Component.literal("Card placed in slot " + getContainerSlot()), false);
                        return true;
                    }

                    var card = CardManager.getCardType(itemStack);
                    var rarity = CardManager.getCardRarity(itemStack);
                    if (card.isEmpty() || rarity.isEmpty()) {
                        return itemStack.isEmpty();
                    }

                    var currentItem = getItem();
                    if (currentItem.isEmpty()) {
                        player.sendSystemMessage(Component.literal("Card placed in slot " + getContainerSlot()), false);
                        return true;
                    }
                    var currentCard = CardManager.getCardType(currentItem);
                    var currentRarity = CardManager.getCardRarity(currentItem);
                    if (currentCard != card || currentRarity != rarity) {
                        player.sendSystemMessage(Component.literal("Card placed in slot " + getContainerSlot()), false);
                    }
                    return currentCard != card || currentRarity != rarity;
                }
            });
        }

        gui.open();
    }
}

