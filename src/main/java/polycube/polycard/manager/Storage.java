package polycube.polycard.manager;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.Rarity;

import java.util.*;

public class Storage {
    public static final int MAX_EQUIPPED_CARDS = 5;
    private final Map<UUID, PlayerData> playerDataMap;
    
    public Storage() {
        this.playerDataMap = new HashMap<>();
    }

    /**
     * Checks if the player has at least one card that is at least as good as the given card.
     * @param player The player to check.
     * @param card The card to compare to.
     * @return True if the player has at least one card that is at least as good as the given card, false otherwise.
     */
    public boolean hasCard(Player player, Card card, Rarity rarity) {
        PlayerData data = data(player);
        return data.containsAtLeast(card, rarity);
    }

    /**
     * Gets the list of equipped cards for a player.
     * @param player The player.
     * @return A list of equipped cards (max 5).
     */
    public List<EquippedCard> getEquippedCards(Player player) {
        return data(player).getEquippedCards();
    }

    /**
     * Equips a card for a player.
     * @param player The player.
     * @param card The card type.
     * @param rarity The card rarity.
     * @return True if the card was equipped, false if the player already has 5 cards equipped.
     */
    public boolean equipCard(Player player, Card card, Rarity rarity) {
        return data(player).equipCard(card, rarity);
    }

    /**
     * Checks if a card type is already equipped by the player.
     * @param player The player.
     * @param card The card type.
     * @return True if the card type is already equipped.
     */
    public boolean hasEquippedCard(Player player, Card card) {
        return data(player).hasEquippedCard(card);
    }

    /**
     * Unequips a card at the given slot for a player.
     * @param player The player.
     * @param slotIndex The slot index (0-4).
     * @return The unequipped card, or null if the slot was empty.
     */
    public EquippedCard unequipCard(Player player, int slotIndex) {
        return data(player).unequipCard(slotIndex);
    }

    /**
     * Clears all equipped cards for a player.
     * @param player The player.
     */
    public void clearEquippedCards(Player player) {
        data(player).clearEquippedCards();
    }

    /**
     * Initialize PlayerData from file
     */
    public static Storage Initialize() {
        // TODO: Load player data from file and populate playerDataMap
        return new Storage();
    }

    public PlayerData data(Player player) {
        return playerDataMap.computeIfAbsent(player.getUUID(), k -> new PlayerData());
    }

    public record EquippedCard(Card card, Rarity rarity) { }

    /**
     * Represents the data associated with a player, including their collection of cards and equipped cards.
     * This class provides methods to add and remove cards, as well as check if the player has at least one card that is at least as good as a given card.
     */
    public static class PlayerData {
        private final List<EquippedCard> equippedCards = new ArrayList<>();
        /**
         * Checks if the player has at least one card that is at least as good as the given card.
         * @param card The card to compare to.
         * @return True if the player has at least one card that is at least as good as the given card, false otherwise.
         */
        public boolean containsAtLeast(Card card, Rarity rarity) {
            return equippedCards.stream().anyMatch(entry -> entry.card() == card && entry.rarity().ordinal() >= rarity.ordinal());
        }

        /**
         * Gets the list of equipped cards for this player.
         * @return A list of equipped cards (max 5).
         */
        public List<EquippedCard> getEquippedCards() {
            return new ArrayList<>(equippedCards);
        }

        /**
         * Equips a card for this player.
         * @param card The card type.
         * @param rarity The card rarity.
         * @return True if the card was equipped, false if the player already has 5 cards equipped.
         */
        public boolean equipCard(Card card, Rarity rarity) {
            if (equippedCards.size() >= MAX_EQUIPPED_CARDS) {
                return false;
            }
            if (hasEquippedCard(card)) {
                return false;
            }
            equippedCards.add(new EquippedCard(card, rarity));
            return true;
        }

        public boolean hasEquippedCard(Card card) {
            return equippedCards.stream().anyMatch(entry -> entry.card() == card);
        }

        /**
         * Unequips a card at the given slot for this player.
         * @param slotIndex The slot index (0-4).
         * @return The unequipped card, or null if the slot was empty.
         */
        public EquippedCard unequipCard(int slotIndex) {
            if (slotIndex < 0 || slotIndex >= equippedCards.size()) {
                return null;
            }
            return equippedCards.remove(slotIndex);
        }

        /**
         * Clears all equipped cards for this player.
         */
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
                        var card = CardManager.getCardType(stack);
                        var rarity = CardManager.getCardRarity(stack);
                        if (card.isPresent() && rarity.isPresent()) {
                            equipCard(card.get(), rarity.get());
                        }
                        PolyCard.LOGGER.info("Slot " + i + ": " + stack.getHoverName().getString());
                    }
                }
            };

            for (EquippedCard equippedCard : equippedCards) {
                var item = CardManager.createCardItem(equippedCard.card(), equippedCard.rarity());
                container.addItem(item);
            }

            return container;
        }
    }
}

