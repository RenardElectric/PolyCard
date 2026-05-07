package polycube.polycard.gui;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import polycube.polycard.manager.CardManager;
import polycube.polycard.manager.Storage;

public class EquipmentGUI {
    private final Storage storage;
    private final CardManager cardManager;

    public EquipmentGUI(Storage storage, CardManager cardManager) {
        this.storage = storage;
        this.cardManager = cardManager;
    }

    /**
     * Opens the equipment GUI for a player - single line with 5 centered slots.
     * @param player The player to open the GUI for.
     */
    public void openEquipmentGUI(Player player) {
        //Inventory gui = Bukkit.createInventory(null, 9, ChatColor.GOLD + "Equipment Slots");

//        // Add empty slots on the left (0-1)
//        for (int i = 0; i < 2; i++) {
//            gui.setItem(i, fillerItem());
//        }
//
//        // Add equipped cards in the middle (2-6) - 5 slots centered
//        List<Storage.EquippedCard> equipped = storage.getEquippedCards(player);
//        for (int i = 0; i < 5; i++) {
//            if (i < equipped.size()) {
//                Storage.EquippedCard equippedCard = equipped.get(i);
//                gui.setItem(2 + i, cardManager.createCardItem(equippedCard.getCard(), equippedCard.getRarity()));
//            } else {
//                gui.setItem(2 + i, null);
//            }
//        }
//
//        // Add empty slots on the right (7-8)
//        for (int i = 7; i < 9; i++) {
//            gui.setItem(i, fillerItem());
//        }

        //player.openInventory(gui);
    }

    /**
     * Creates a filler item (gray stained glass pane with no name)
     */
    private ItemStack fillerItem() {
        ItemStack item = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        item.set(DataComponents.ITEM_NAME, Component.literal(" "));
        return item;
    }

}

