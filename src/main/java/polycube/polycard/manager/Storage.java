package polycube.polycard.manager;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.utils.Helpers;

import java.util.*;

/// Manages the storage of player card data for the PolyCard mod,
/// allowing players to equip and unequip cards
/// and saving this data persistently on the server.
public class Storage extends SavedData {

    public static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    public static final Codec<Storage> CODEC = Codec.unboundedMap(UUID_CODEC, PlayerData.CODEC)
            .xmap(Storage::new, Storage::getPlayerDataMap);

    private static final SavedDataType<Storage> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, "storage"),
            () -> new Storage(new HashMap<>()),
            CODEC,
            null
    );

    public static final int MAX_EQUIPPED_CARDS = 5;

    private final Map<UUID, PlayerData> playerDataMap;

    public Storage(Map<UUID, PlayerData> playerDataMap) {
        this.playerDataMap = new HashMap<>(playerDataMap);
    }

    /// Returns the map of player UUIDs to their corresponding PlayerData.
    ///
    /// @return the map of player UUIDs to PlayerData
    public Map<UUID, PlayerData> getPlayerDataMap() {
        return playerDataMap;
    }

    /// Retrieves the saved Storage instance from the server's data storage,
    /// or creates a new one if it doesn't exist.
    ///
    /// @param server the Minecraft server to retrieve the storage from
    /// @return the Storage instance associated with the server
    public static Storage getSavedStorage(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    /// Retrieves the PlayerData for a given player,
    /// creating a new entry if it doesn't exist.
    ///
    /// @param player the player to retrieve the data for
    /// @return the PlayerData associated with the player
    public PlayerData data(Player player) {
        return playerDataMap.computeIfAbsent(player.getUUID(), _ -> new PlayerData(new ArrayList<>(MAX_EQUIPPED_CARDS)));
    }

    /// Manages the card data for a single player, including equipped cards and related operations.
    public record PlayerData(List<Card> equippedCards) {
        public static final Codec<PlayerData> CODEC = Card.CODEC.listOf().xmap(PlayerData::new, PlayerData::getEquippedCards);

        public PlayerData {
            equippedCards = new ArrayList<>(equippedCards);
        }

        /// Checks if the player has a specific card equipped.
        ///
        /// @param card the card to check for
        /// @return true if the card is equipped, false otherwise
        public boolean hasCard(Card card) {
            return equippedCards.contains(card);
        }

        /// Checks if the player has a card of a specific type and rarity level equipped.
        ///
        /// @param cardType    the type of card to check for
        /// @param rarityLevel the rarity level of card to check for
        /// @return true if a card of the specified type and rarity is equipped, false otherwise
        public boolean hasCard(CardType cardType, RarityLevel rarityLevel) {
            return equippedCards.stream().anyMatch(equippedCard -> equippedCard.cardType() == cardType && equippedCard.rarityLevel() == rarityLevel);
        }

        /// Checks if the player has a card of a specific card with same or higher rarity level equipped.
        ///
        /// @param card the card to check for
        /// @return true if a card of the same type and same or higher rarity level is equipped, false otherwise
        public boolean hasCardOrRarer(Card card) {
            return equippedCards.stream().anyMatch(equippedCard -> equippedCard.cardType() == card.cardType() && equippedCard.rarityLevel().ordinal() >= card.rarityLevel().ordinal());
        }

        /// Checks if the player has a card of a specific type with same or higher rarity level equipped.
        ///
        /// @param cardType    the type of card to check for
        /// @param rarityLevel the rarity level of card to check for
        /// @return true if a card of the specified type and same or higher rarity level is equipped, false otherwise
        public boolean hasCardOrRarer(CardType cardType, RarityLevel rarityLevel) {
            return equippedCards.stream().anyMatch(equippedCard -> equippedCard.cardType() == cardType && equippedCard.rarityLevel().ordinal() >= rarityLevel.ordinal());
        }

        /// Checks if the player has a card of a specific type equipped.
        ///
        /// @param cardType the type of card to check for
        /// @return true if a card of the specified type is equipped, false otherwise
        public boolean hasCardType(CardType cardType) {
            return equippedCards.stream().anyMatch(equippedCard -> equippedCard.cardType() == cardType);
        }

        /// Returns the list of cards currently equipped by the player.
        ///
        /// @return the list of equipped cards
        public List<Card> getEquippedCards() {
            return equippedCards;
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

        /// Converts the player's equipped cards into a Container that can be used in the game's UI,
        /// allowing the player to view and manage their equipped cards.
        ///
        /// @return a Container representing the player's equipped cards
        public Container asContainer(ServerPlayer player) {
            var container = new SimpleContainer(MAX_EQUIPPED_CARDS) {
                @Override
                public void setChanged() {
                    List<Card> cardInContainer = items.stream()
                            .map(Card::getCard)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .toList();

                    List<Card> removedCards = equippedCards.stream()
                            .filter(card -> !cardInContainer.contains(card))
                            .toList();

                    List<Card> addedCards = cardInContainer.stream()
                            .filter(card -> !equippedCards.contains(card))
                            .toList();

                    for (Card card : removedCards) {
                        Helpers.debug("Unequipped card {} from container", card);
                        CardManager.removeCardAttributes(player, card);
                        equippedCards.remove(card);
                        Helpers.playSound(player, SoundEvents.BUNDLE_REMOVE_ONE);
                    }

                    for (Card card : addedCards) {
                        Helpers.debug("Equipped card {} to container", card);
                        CardManager.addCardAttributes(player, card);
                        equippedCards.add(card);
                        Helpers.playSound(player, SoundEvents.BUNDLE_INSERT);
                    }
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
    }
}

