package polycube.polycard.data;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.utils.CardHelper;
import polycube.polycard.utils.Helpers;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/// Manages the card data for a single player, including equipped cards and related operations.
public class PlayerData {
    public static final int MAX_EQUIPPED_CARDS = 5;
    public static final Codec<PlayerData> CODEC = Card.CODEC.listOf().xmap(PlayerData::new, PlayerData::getEquippedCards);

    private final Set<Card> equippedCards;

    public PlayerData(List<Card> equippedCards) {
        this.equippedCards = new HashSet<>(equippedCards);
    }

    public List<Card> getEquippedCards() {
        return List.copyOf(equippedCards);
    }

    /// Checks if the player has a card of a specific type and rarity level equipped.
    ///
    /// @param cardType    the type of card to check for
    /// @param rarityLevel the rarity level of card to check for
    /// @return true if a card of the specified type and rarity is equipped, false otherwise
    public boolean hasCard(CardType cardType, RarityLevel rarityLevel) {
        return hasCard(new Card(cardType, rarityLevel));
    }

    /// Checks if the player has a specific card equipped.
    ///
    /// @param card the card to check for
    /// @return true if the card is equipped, false otherwise
    public boolean hasCard(Card card) {
        return equippedCards.contains(card);
    }

    /// Checks if the player has a card of a specific card with same or higher rarity level equipped.
    ///
    /// @param card the card to check for
    /// @return true if a card of the same type and same or higher rarity level is equipped, false otherwise
    public boolean hasCardOrRarer(Card card) {
        return hasCardOrRarer(card.cardType(), card.rarityLevel());
    }

    /// Checks if the player has a card of a specific type with same or higher rarity level equipped.
    ///
    /// @param cardType    the type of card to check for
    /// @param rarityLevel the rarity level of card to check for
    /// @return true if a card of the specified type and same or higher rarity level is equipped, false otherwise
    public boolean hasCardOrRarer(CardType cardType, RarityLevel rarityLevel) {
        for (Card equippedCard : equippedCards) {
            if (equippedCard.cardType() == cardType &&
                    equippedCard.rarityLevel().ordinal() >= rarityLevel.ordinal()
            ) {
                return true;
            }
        }
        return false;
    }

    /// Checks if the player has a card of a specific type equipped.
    ///
    /// @param cardType the type of card to check for
    /// @return true if a card of the specified type is equipped, false otherwise
    public boolean hasCardType(CardType cardType) {
        for (Card equippedCard : equippedCards) {
            if (equippedCard.cardType() == cardType) {
                return true;
            }
        }
        return false;
    }

    /// Attempts to equip a card for the player,
    /// ensuring that the maximum number of equipped cards is not exceeded
    /// and that the player does not already have a card of the same type equipped.
    ///
    /// @param card the card to equip
    /// @return true if the card was successfully equipped, false otherwise
    public boolean equipCard(Card card) {
        if (equippedCards.size() >= MAX_EQUIPPED_CARDS) {
            return false;
        }
        if (hasCardType(card.cardType())) {
            return false;
        }
        equippedCards.add(card);
        return true;
    }

    /// Attempts to unequip a specific card from the player's equipped cards.
    ///
    /// @param card the card to unequip
    /// @return true if the card was successfully unequipped, false otherwise
    public boolean unequipCard(Card card) {
        return equippedCards.remove(card);
    }

    /// Attempts to unequip any card of a specific type from the player's equipped cards.
    ///
    /// @param cardType the type of card to unequip
    /// @return true if a card of the specified type was successfully unequipped, false otherwise
    public boolean unequipCardType(CardType cardType) {
        return equippedCards.removeIf(equippedCard -> equippedCard.cardType() == cardType);
    }

    /// Clears all equipped cards from the player's data.
    public void clearEquippedCards() {
        equippedCards.clear();
    }

    /// Returns a Container representing the player's equipped cards, allowing for interaction with the GUI.
    ///
    /// @param player the player for whom to create the container
    /// @return a Container representing the player's equipped cards
    public Container asContainer(ServerPlayer player) {
        return asContainer(player, player);
    }

    /// Returns a Container representing the player's equipped cards, allowing for interaction with the GUI.
    /// This version allows specifying a different player for feedback sounds when equipping or unequipping cards.
    ///
    /// @param targetPlayer   the player whose equipped cards are represented in the container
    /// @param feedbackPlayer the player who will receive feedback sounds when equipping or unequipping cards
    /// @return a Container representing the player's equipped cards
    public Container asContainer(ServerPlayer targetPlayer, ServerPlayer feedbackPlayer) {
        var container = new SimpleContainer(MAX_EQUIPPED_CARDS) {
            @Override
            public void setChanged() {
                var cardsInContainer = items.stream()
                        .map(Card::getCard)
                        .flatMap(Optional::stream)
                        .collect(Collectors.toSet());

                if (equippedCards.equals(cardsInContainer)) {
                    return;
                }

                for (var card : equippedCards) {
                    if (!cardsInContainer.contains(card)) {
                        Helpers.debug("{} unequipped card {} for {}", feedbackPlayer, card, targetPlayer);
                        CardHelper.removeCardAttributes(targetPlayer, card);
                        Helpers.playSound(feedbackPlayer, SoundEvents.BUNDLE_REMOVE_ONE);
                    }
                }

                for (var card : cardsInContainer) {
                    if (!equippedCards.contains(card)) {
                        Helpers.debug("{} equipped card {} for {}", feedbackPlayer, card, targetPlayer);
                        CardHelper.addCardAttributes(targetPlayer, card);
                        Helpers.playSound(feedbackPlayer, SoundEvents.BUNDLE_INSERT);
                    }
                }

                equippedCards.clear();
                equippedCards.addAll(cardsInContainer);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        };

        int index = 0;
        for (Card equippedCard : equippedCards) {
            container.items.set(index++, equippedCard.asItem());
        }

        return container;
    }

    /// Checks if the player has a card of a specific type with same or higher rarity level equipped.
    ///
    /// @param cardType    the type of card to check for
    /// @param rarityLevel the rarity level of card to check for
    /// @return true if a card of the specified type and same or higher rarity level is equipped, false otherwise
    public static boolean hasCardOrRarer(ServerPlayer player, CardType cardType, RarityLevel rarityLevel) {
        return PolyCard.STORAGE.getPlayerData(player).hasCardOrRarer(cardType, rarityLevel);
    }
}
