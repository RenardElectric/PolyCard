package polycube.polycard.gui;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/// A vanilla shulker box screen backed by a shulker box item sitting in the player's inventory.
///
/// Every change is written straight back to the item. The item's own slot cannot be clicked,
/// swapped or thrown, and every click first checks the item is still in that inventory slot, so
/// the contents can never exist both in the menu and in a moved or dropped copy of the item.
public class ItemShulkerBoxMenu extends ShulkerBoxMenu {
    public static final int CONTAINER_SIZE = 27;
    private static final int MAIN_INVENTORY_MENU_SLOT_START = CONTAINER_SIZE;
    private static final int HOTBAR_MENU_SLOT_START = CONTAINER_SIZE + 27;

    private final ItemStack shulkerStack;
    private final int inventorySlot;

    private ItemShulkerBoxMenu(int containerId, Inventory inventory, int inventorySlot, ItemStack shulkerStack, ItemBackedContainer container) {
        super(containerId, inventory, container);
        this.shulkerStack = shulkerStack;
        this.inventorySlot = inventorySlot;
    }

    /// Creates a menu for the shulker box item in an inventory slot: hotbar (0-8), main inventory (9-35) or offhand.
    public static ItemShulkerBoxMenu create(int containerId, Inventory inventory, int inventorySlot) {
        if (!isSupportedInventorySlot(inventorySlot)) {
            throw new IllegalArgumentException("Unsupported shulker box inventory slot " + inventorySlot);
        }
        var shulkerStack = inventory.getItem(inventorySlot);
        return new ItemShulkerBoxMenu(containerId, inventory, inventorySlot, shulkerStack, new ItemBackedContainer(shulkerStack));
    }

    public static boolean isSupportedInventorySlot(int inventorySlot) {
        return inventorySlot >= 0 && inventorySlot < Inventory.INVENTORY_SIZE || inventorySlot == Inventory.SLOT_OFFHAND;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (!stillValid(player)) {
            if (player instanceof ServerPlayer serverPlayer) serverPlayer.closeContainer();
            return;
        }
        if (slotId == lockedMenuSlot() || input == ContainerInput.SWAP && button == inventorySlot) {
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && !shulkerStack.isEmpty() && player.getInventory().getItem(inventorySlot) == shulkerStack;
    }

    /// Returns the menu slot showing the shulker box item, or -1 when it is not shown (offhand).
    private int lockedMenuSlot() {
        if (Inventory.isHotbarSlot(inventorySlot)) {
            return HOTBAR_MENU_SLOT_START + inventorySlot;
        }
        if (inventorySlot < Inventory.INVENTORY_SIZE) {
            return MAIN_INVENTORY_MENU_SLOT_START + inventorySlot - Inventory.getSelectionSize();
        }
        return -1;
    }

    /// A 27-slot container that mirrors its contents into the shulker box item after every change.
    private static final class ItemBackedContainer extends SimpleContainer {
        private final ItemStack shulkerStack;

        private ItemBackedContainer(ItemStack shulkerStack) {
            super(CONTAINER_SIZE);
            this.shulkerStack = shulkerStack;
            var items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
            shulkerStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                getItems().set(i, items.get(i));
            }
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (!shulkerStack.isEmpty()) {
                shulkerStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems()));
            }
        }
    }
}
