package polycube.polycard.manager;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;

import java.util.*;

public class Storage {
    public static final int MAX_EQUIPPED_CARDS = 5;
    private final Map<UUID, PlayerData> playerDataMap;
    
    public Storage() {
        this.playerDataMap = new HashMap<>();
    }

    public boolean hasCard(Player player, Card card) {
        return data(player).hasCard(card);
    }

    public boolean hasCardOrRarer(Player player, Card card) {
        return data(player).hasCardOrRarer(card);
    }

    /// Gets the list of equipped cards for a player.
    ///
    /// @param player The player.
    /// @return A list of equipped cards (max 5).
    public List<Card> getEquippedCards(Player player) {
        return data(player).getEquippedCards();
    }

    /// Equips a card for a player.
    ///
    /// @param player The player.
    /// @param card The card.
    /// @return True if the card was equipped, false otherwise.
    public boolean equipCard(Player player, Card card) {
        return data(player).equipCard(card);
    }

    /// Checks if a card type is already equipped by the player.
    ///
    /// @param player The player.
    /// @param card The card type.
    /// @return True if the card type is already equipped.
    public boolean hasCardType(Player player, CardType card) {
        return data(player).hasCardType(card);
    }

    public boolean unequipCard(Player player, Card card) {
        return data(player).unequipCard(card);
    }

    public boolean unequipCardType(Player player, CardType cardType) {
        return data(player).unequipCardType(cardType);
    }

    /// Clears all equipped cards for a player.
    /// @param player The player.
    public void clearEquippedCards(Player player) {
        data(player).clearEquippedCards();
    }

    /// Initialize PlayerData from file
    public static Storage Initialize() {
        // TODO: Load player data from file and populate playerDataMap
        return new Storage();
    }

    public PlayerData data(Player player) {
        return playerDataMap.computeIfAbsent(player.getUUID(), _ -> new PlayerData());
    }

    /// Represents the data associated with a player, including their collection of cards and equipped cards.
    ///
    /// This class provides methods to add and remove cards, as well as check if the player has at least one card that is at least as good as a given card.
    public static class PlayerData {
        private final Set<Card> equippedCards = new HashSet<>();

        public boolean hasCard(Card card) {
            return equippedCards.contains(card);
        }

        public boolean hasCardOrRarer(Card card) {
            return equippedCards.stream().anyMatch(equippedCard -> equippedCard.type() == card.type() && equippedCard.rarity().ordinal() >= card.rarity().ordinal());
        }

        public boolean hasCardType(CardType cardType) {
            return equippedCards.stream().anyMatch(equippedCard -> equippedCard.type() == cardType);
        }

        /// Gets the list of equipped cards for this player.
        ///
        /// @return A list of equipped cards (max 5).
        public List<Card> getEquippedCards() {
            return new ArrayList<>(equippedCards);
        }

        /// Equips a card for this player.
        ///
        /// @param card The card type.
        /// @return True if the card was equipped, false if the player already has 5 cards equipped.
        public boolean equipCard(Card card) {
            if (equippedCards.size() >= MAX_EQUIPPED_CARDS) {
                return false;
            }
            if (hasCardType(card.type())) {
                return false;
            }
            equippedCards.add(card);
            return true;
        }

        public boolean unequipCard(Card card) {
            return equippedCards.remove(card);
        }

        public boolean unequipCardType(CardType cardType) {
            return equippedCards.removeIf(equippedCard -> equippedCard.type() == cardType);
        }

        /// Clears all equipped cards for this player.
        public void clearEquippedCards() {
            equippedCards.clear();
        }

        public Container asContainer() {
            var container = new SimpleContainer(MAX_EQUIPPED_CARDS) {
                @Override
                public void setChanged() {
                    clearEquippedCards();
                    for (int i = 0; i < getContainerSize(); i++) {
                        ItemStack stack = getItem(i);
                        CardManager.getCard(stack)
                                .ifPresent(value -> equipCard(value));
                    }
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            };

            int index = 0;
            for (Card equippedCard : equippedCards) {
                var item = CardManager.createCardItem(equippedCard);
                container.items.set(index++, item);
            }

            return container;
        }
    }
}

