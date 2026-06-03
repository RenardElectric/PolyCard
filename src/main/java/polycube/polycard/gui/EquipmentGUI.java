package polycube.polycard.gui;

import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.data.PlayerData;
import polycube.polycard.utils.Helpers;

import java.util.List;
import java.util.stream.Collectors;

/// Manages the equipment GUI for players to equip and unequip cards in the PolyCard mod,
/// allowing them to see their currently equipped cards and manage them in a user-friendly interface.
public class EquipmentGUI {
    /// Opens the equipment GUI for a player, showing their own equipped cards.
    ///
    /// @param player The player to open the GUI for.
    public static void openEquipmentGUI(ServerPlayer player) {
        openEquipmentGUI(player, player);
    }

    /// Opens the equipment GUI for a player - single line with 5 centered slots.
    ///
    /// @param player       The player to open the GUI for.
    /// @param targetPlayer The player whose equipment is being managed (can be the player).
    public static void openEquipmentGUI(ServerPlayer player, ServerPlayer targetPlayer) {
    var playerData = PolyCard.STORAGE.getPlayerData(targetPlayer);
        var container = playerData.asContainer(targetPlayer, player);

        SimpleGui gui = getEquipmentGui(player, targetPlayer, playerData);
        gui.setTitle(Component.literal("七ㇺ十").withStyle(ChatFormatting.WHITE)
                .append(Component.literal("✦༺ ").withStyle(ChatFormatting.DARK_RED))
                .append(Component.literal("Equipped Cards").withStyle(s -> s.withColor(ChatFormatting.BLACK).withUnderlined(true)))
                .append(Component.literal(" ༻✦").withStyle(ChatFormatting.DARK_RED))
        );

        for (int i = 0; i < 5; i++) {
            gui.setSlot(i, getSlot(container, player, playerData, i));
        }

        gui.open();
    }

    private static SimpleGui getEquipmentGui(ServerPlayer viewer, ServerPlayer targetPlayer, PlayerData playerData) {
        return new SimpleGui(MenuType.HOPPER, viewer, false) {
            @Override
            public void onPlayerClose(boolean success) {
                super.onPlayerClose(success);

                if (!success) {
                    Helpers.debug("{} could not close the equipment menu for {} successfully, not saving equipped cards", viewer.getName().getString(), targetPlayer.getName().getString());
                    return;
                }

                List<Card> equippedCards = playerData.getEquippedCards();

                Helpers.debug("Saved equipped cards for {} after closing equipment menu", targetPlayer.getName().getString());
                if (equippedCards.isEmpty()) {
                    Helpers.debug("No equipped cards for {}", targetPlayer.getName().getString());
                } else {
                    Helpers.debug(
                            "Equipped cards for {}: {}", targetPlayer.getName().getString(),
                            equippedCards.stream().map(Card::toString).collect(Collectors.joining(", "))
                    );
                }
                PolyCard.STORAGE.save();
            }
        };
    }

    private static Slot getSlot(Container container, ServerPlayer player, PlayerData playerData, int slot) {
        return new Slot(container, slot, 0, 0) {
            @Override
            public boolean mayPlace(@NonNull ItemStack itemStack) {
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
                    player.sendSystemMessage(Component.literal("You cannot equip the same card type twice.").withStyle(ChatFormatting.RED));  // TODO: Not sure, makes a lot of messages when shift clicking
                    Helpers.playFailure(player);
                    Helpers.debug("{} attempted to equip duplicate card type: {}", player.getName().getString(), card.cardType().name());
                    return false;
                }

                Helpers.debug("{} is equipping card {} in slot {}", player.getName().getString(), card, getContainerSlot());
                return true;
            }
        };
    }
}

