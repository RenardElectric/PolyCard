package polycube.polycard.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.events.callBacks.CardEventCallback;
import polycube.polycard.utils.Helpers;

import java.util.*;

/// Persistent card equipment for one player.
/// Internally, cards are stored as a compact type-to-rarity map; public card operations use validated Card values.
public record PlayerData(Map<CardType, RarityLevel> equippedCards) {
    public static final int MAX_EQUIPPED_CARDS = 5;
    public static final Codec<PlayerData> CODEC = Codec.dispatchedMap(CardType.CODEC, _ -> RarityLevel.CODEC).xmap(PlayerData::new, PlayerData::equippedCards);

    public PlayerData() {
        this(new EnumMap<>(CardType.class));
    }

    public PlayerData(Map<CardType, RarityLevel> equippedCards) {
        this.equippedCards = new EnumMap<>(CardType.class);
        for (var cardType : CardType.values()) {
            var rarityLevel = equippedCards.get(cardType);
            if (rarityLevel != null && Card.tryCreate(cardType, rarityLevel).isPresent()) {
                if (this.equippedCards.size() == MAX_EQUIPPED_CARDS) {
                    break;
                }
                this.equippedCards.put(cardType, rarityLevel);
            }
        }
        int ignoredEntries = equippedCards.size() - this.equippedCards.size();
        if (ignoredEntries > 0) {
            Helpers.debug("Ignored {} invalid or excess equipped-card {} while loading player data",
                    ignoredEntries, ignoredEntries == 1 ? "entry" : "entries");
        }
    }

    /// Exposes equipped cards as a read-only map.
    @Override
    public Map<CardType, RarityLevel> equippedCards() {
        return Collections.unmodifiableMap(equippedCards);
    }

    /// Returns the equipped rarity without allocating a read-only map wrapper on hot event paths.
    public @Nullable RarityLevel equippedRarityLevel(CardType cardType) {
        return equippedCards.get(cardType);
    }

    /// Returns the number of equipped cards.
    public int equippedCardCount() {
        return equippedCards.size();
    }

    /// Returns equipped cards as concrete, validated Card instances.
    public List<Card> getEquippedCards() {
        var cards = new ArrayList<Card>(equippedCards.size());
        for (var entry : equippedCards.entrySet()) {
            cards.add(new Card(entry.getKey(), entry.getValue()));
        }
        return cards;
    }

    /// Returns whether this exact type/rarity pair is equipped.
    @SuppressWarnings("unused")
    public boolean hasCard(CardType cardType, RarityLevel rarityLevel) {
        return equippedCards.get(cardType) == rarityLevel;
    }

    /// Returns whether the equipped card for this type is at least the requested rarity.
    public boolean hasCardOrRarer(CardType cardType, RarityLevel rarityLevel) {
        var storedRarityLevel = equippedCards.get(cardType);
        return storedRarityLevel != null && storedRarityLevel.isAtLeast(rarityLevel);
    }

    /// Returns whether any rarity of this card type is equipped.
    public boolean hasCardType(CardType cardType) {
        return equippedCards.containsKey(cardType);
    }

    public DataResult<PlayerData> canEquipCardType(CardType cardType) {
        if (equippedCardCount() >= MAX_EQUIPPED_CARDS) {
            return DataResult.error(() -> "there are already " + MAX_EQUIPPED_CARDS + " cards equipped");
        }
        if (hasCardType(cardType)) {
            return DataResult.error(() -> cardType + " is already equipped");
        }

        var mutexGroup = cardType.getMutexGroup();
        if (!mutexGroup.isBlank())
            for (var equippedType : equippedCards.keySet())
                if (equippedType.getMutexGroup().equals(mutexGroup))
                    return DataResult.error(() -> "cannot equip " + cardType + " because a " + mutexGroup + " card is already equipped");

        return DataResult.success(this);
    }

    /// Equips a card if there is room and no card of the same type is yet equipped.
    public static DataResult<PlayerData> equipCard(ServerPlayer player, Card card) {
        return PolyCard.storage().getPlayerData(player).canEquipCardType(card.cardType()).map(playerData -> {
            playerData.equippedCards.put(card.cardType(), card.rarityLevel());
            CardEventCallback.EQUIPPED.invoker().onCardEquip(player, card);
            PolyCard.storage().setDirty();
            return playerData;
        });
    }

    /// Unequips a card if it is currently equipped.
    public static DataResult<PlayerData> unequipCard(ServerPlayer player, Card card) {
        var playerData = PolyCard.storage().getPlayerData(player);
        var cardType = card.cardType();
        var removedRarityLevel = playerData.equippedCards.remove(cardType);
        if (removedRarityLevel == card.rarityLevel()) {
            CardEventCallback.UNEQUIPPED.invoker().onCardUnequip(player, card);
            PolyCard.storage().setDirty();
            return DataResult.success(playerData);
        } else if (removedRarityLevel != null) {
            playerData.equippedCards.put(cardType, removedRarityLevel);
        }
        return DataResult.error(() -> cardType + " is not equipped");
    }
}
