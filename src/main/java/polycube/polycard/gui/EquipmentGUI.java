package polycube.polycard.gui;

import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.data.PlayerData;
import polycube.polycard.utils.Helpers;

/// Server-side card equipment GUI backed by PlayerData's syncing container.
public final class EquipmentGUI {
    private EquipmentGUI() {
    }

    /// Opens a player's own equipment manager.
    public static void openEquipmentGUI(ServerPlayer player) {
        openEquipmentGUI(player, player);
    }

    /// Opens an equipment manager where viewer edits targetPlayer's cards.
    public static void openEquipmentGUI(ServerPlayer viewer, ServerPlayer targetPlayer) {
        var playerData = PolyCard.storage().getPlayerData(targetPlayer);
        var container = playerData.asContainer(targetPlayer, viewer);

        SimpleGui gui = new SimpleGui(MenuType.HOPPER, viewer, false);
        gui.setTitle(Component.literal("七ㇺ十").withStyle(ChatFormatting.WHITE)
                .append(Component.literal("✦༺ ").withStyle(ChatFormatting.DARK_RED))
                .append(Component.literal("Equipped Cards").withStyle(s -> s.withColor(ChatFormatting.BLACK).withUnderlined(true)))
                .append(Component.literal(" ༻✦").withStyle(ChatFormatting.DARK_RED))
        );

        for (int i = 0; i < PlayerData.MAX_EQUIPPED_CARDS; i++) {
            gui.setSlot(i, getSlot(container, viewer, playerData, i));
        }

        gui.open();
        Helpers.debug("{} opened the equipment manager for {}", viewer.getName().getString(), targetPlayer.getName().getString());
    }

    private static Slot getSlot(Container container, ServerPlayer player, PlayerData playerData, int slot) {
        return new Slot(container, slot, 0, 0) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                if (itemStack.isEmpty()) {
                    Helpers.debug("{} attempted to place empty item in equipment slot {}", player.getName().getString(), getContainerSlot());
                    return false;
                }

                var optionalCard = Card.getCard(itemStack);
                if (optionalCard.isEmpty()) {
                    Helpers.playFailure(player);
                    Helpers.debug("{} attempted to place non-card item in equipment slot: {}", player.getName().getString(), itemStack.getHoverName().getString());
                    return false;
                }
                var card = optionalCard.get();

                var currentItem = getItem();
                if (!currentItem.isEmpty()) {
                    var currentCard = Card.getCard(currentItem);
                    if (currentCard.isPresent() && currentCard.get().cardType() == card.cardType()) {
                        Helpers.debug("{} is replacing card {} in slot {} with {}", player.getName().getString(), currentCard.get(), getContainerSlot(), card);
                        return true;
                    }
                }

                if (playerData.hasCardType(card.cardType())) {
                    player.sendSystemMessage(Component.literal("You cannot equip the same card type twice.").withStyle(ChatFormatting.RED));
                    Helpers.playFailure(player);
                    Helpers.debug("{} attempted to equip duplicate card type: {}", player.getName().getString(), card.cardType());
                    return false;
                }

                Helpers.debug("{} is equipping card {} in slot {}", player.getName().getString(), card, getContainerSlot());
                return true;
            }
        };
    }
}

