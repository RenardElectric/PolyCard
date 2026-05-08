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

    public void openEquipmentGUI(ServerPlayer player) {
        openEquipmentGUI(player, player);
    }

    /// Opens the equipment GUI for a player - single line with 5 centered slots.
    ///
    /// @param player The player to open the GUI for.
    /// @param targetPlayer The player whose equipment is being managed (can be the same as player).
    public void openEquipmentGUI(ServerPlayer player, ServerPlayer targetPlayer) {
        var playerData = storage.data(targetPlayer);
        var container = playerData.asContainer();

        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x1, player, false) {
            @Override
            public void onPlayerClose(boolean success) {
                super.onPlayerClose(success);

                if (!success) {
                    PolyCard.debug("{} could not close the equipment menu successfully, not saving equipped cards", player.getName().getString());
                    return;
                }

                List<Card> equippedCards = playerData.getEquippedCards();

                PolyCard.debug("Saved equipped cards for {} after closing equipment menu", player.getName().getString());
                if (equippedCards.isEmpty()) {
                    player.sendSystemMessage(Component.literal(ChatFormatting.GOLD + "Equipped cards: " + ChatFormatting.GRAY + "none"));
                    PolyCard.debug("No equipped cards for {}", player.getName().getString());
                } else {
                    player.sendSystemMessage(Component.literal(ChatFormatting.GOLD + "Equipped cards: ").append(
                            ComponentUtils.formatList(equippedCards, Card::getFormatedName)
                    ));
                    PolyCard.debug(
                            "Equipped cards for {}: {}", player.getName().getString(),
                            new StringJoiner(", ").add(equippedCards.stream().map(Card::getFormatedName).toList().toString()).toString()
                    );
                }
            }
        };

        gui.setTitle(Component.literal("Equipment Slots").withStyle(ChatFormatting.GOLD));

        for (int i = 0; i < 5; i++) {
            gui.setSlot(2 + i, new Slot(container, i, 0, 0) {
                @Override
                public boolean mayPlace(@NonNull ItemStack itemStack) {
                    if (itemStack.isEmpty()) {
                        PolyCard.debug("{} attempted to place empty item in equipment slot {}", player.getName().getString(), getContainerSlot());
                        return false;
                    }

                    var optionalCard = CardManager.getCard(itemStack);
                    if (optionalCard.isEmpty()) {
                        PolyCard.debug("{} attempted to place non-card item in equipment slot: {}", player.getName().getString(), itemStack.getHoverName().getString());
                        return false;
                    }
                    var card = optionalCard.get();

                    var currentItem = getItem();
                    if (!currentItem.isEmpty()) {
                        var currentCard = CardManager.getCard(currentItem);
                        if (currentCard.isPresent() && currentCard.get().type() == card.type()) {
                            PolyCard.debug("{} is replacing card {} in slot {} with {}", player.getName().getString(), currentCard.get().getDisplayName(), getContainerSlot(), card.getDisplayName());
                            return true;
                        }
                    }

                    if (playerData.hasCardType(card.type())) {
                        player.sendSystemMessage(Component.literal("You cannot equip the same card type twice.").withStyle(ChatFormatting.RED));
                        PolyCard.debug("{} attempted to equip duplicate card type: {}", player.getName().getString(), card.type().name());
                        return false;
                    }

                    PolyCard.debug("{} is equipping card {} in slot {}", player.getName().getString(), card.getDisplayName(), getContainerSlot());
                    return true;
                }
            });
        }

        gui.open();
    }
}

