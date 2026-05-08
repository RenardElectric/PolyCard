package polycube.polycard.gui;

import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.manager.CardManager;
import polycube.polycard.manager.Storage;

import java.util.List;
import java.util.StringJoiner;

public class EquipmentGUI {
    private final Storage storage;

    public EquipmentGUI(Storage storage) {
        this.storage = storage;
    }

    /// Opens the equipment GUI for a player - single line with 5 centered slots.
    ///
    /// @param player The player to open the GUI for.
    public void openEquipmentGUI(ServerPlayer player) {
        var playerData = storage.data(player);
        var container = playerData.asContainer();

        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x1, player, false) {
            @Override
            public void onPlayerClose(boolean success) {
                super.onPlayerClose(success);

                if (!success) return;

                List<Card> equippedCards = playerData.getEquippedCards();

                if (equippedCards.isEmpty()) {
                    player.sendSystemMessage(Component.literal(ChatFormatting.GOLD + "Equipped cards: " + ChatFormatting.GRAY + "none"));
                } else {
                    player.sendSystemMessage(Component.literal(ChatFormatting.GOLD + "Equipped cards: ").append(
                            ComponentUtils.formatList(equippedCards, Card::getFormatedName)
                    ));
                }
                PolyCard.LOGGER.debug("[Polycard] Saved equipped cards for {} after closing equipment menu", player.getName().getString());
            }
        };

        gui.setTitle(Component.literal("Equipment Slots").withStyle(ChatFormatting.GOLD));

        for (int i = 0; i < 5; i++) {
            gui.setSlot(2 + i, new Slot(container, i, 0, 0) {
                @Override
                public boolean mayPlace(@NonNull ItemStack itemStack) {
                    if (itemStack.isEmpty()) {
                        return false;
                    }

                    var optionalCard = CardManager.getCard(itemStack);
                    if (optionalCard.isEmpty()) {
                        return false;
                    }
                    var card = optionalCard.get();

                    var currentItem = getItem();
                    if (!currentItem.isEmpty()) {
                        var currentCard = CardManager.getCardType(currentItem);
                        if (currentCard.isPresent() && currentCard.get() == card.type()) {
                            return true;
                        }
                    }

                    if (playerData.hasCardType(card.type())) {
                        player.sendSystemMessage(Component.literal("You cannot equip the same card type twice.").withStyle(ChatFormatting.RED));
                        PolyCard.LOGGER.debug("[Polycard] {} attempted to equip duplicate card type: {}", player.getName().getString(), card.type().name());
                        return false;
                    }
                    return true;
                }
            });
        }

        gui.open();
    }
}

