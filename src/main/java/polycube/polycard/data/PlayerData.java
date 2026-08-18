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
        var mutexGroups = new HashSet<String>();
        for (var cardType : CardType.values()) {
            var rarityLevel = equippedCards.get(cardType);
            var mutexGroup = cardType.getMutexGroup();
            boolean mutexAvailable = mutexGroup.isBlank() || !mutexGroups.contains(mutexGroup);
            if (rarityLevel != null && mutexAvailable && Card.tryCreate(cardType, rarityLevel).isPresent()) {
                if (this.equippedCards.size() == MAX_EQUIPPED_CARDS) {
                    break;
                }
                this.equippedCards.put(cardType, rarityLevel);
                if (!mutexGroup.isBlank()) {
                    mutexGroups.add(mutexGroup);
                }
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
        return cardsFrom(equippedCards);
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

    /// Validates a complete proposed equipment set without changing the persistent state.
    public DataResult<List<Card>> canSetEquippedCards(Collection<Card> cards) {
        return validateEquipment(cards).map(PlayerData::cardsFrom);
    }

    /// Equips a card, atomically replacing a different rarity of the same or mutex-group card.
    public static DataResult<EquipmentChange> equipOrReplaceCard(ServerPlayer player, Card card) {
        var playerData = PolyCard.storage().getPlayerData(player);
        var target = playerData.getEquippedCards();
        var mutexGroup = card.cardType().getMutexGroup();
        var replacedCard = target.stream()
                .filter(equippedCard -> equippedCard.cardType() == card.cardType()
                        || (!mutexGroup.isBlank() && equippedCard.cardType().getMutexGroup().equals(mutexGroup)))
                .findFirst();

        if (replacedCard.filter(card::equals).isPresent()) {
            return DataResult.error(() -> card.cardType() + " is already equipped");
        }
        replacedCard.ifPresent(target::remove);
        target.add(card);
        return applyEquipment(player, target);
    }

    /// Atomically replaces the complete equipment set after validating all invariants.
    public static DataResult<EquipmentChange> setEquippedCards(ServerPlayer player, Collection<Card> cards) {
        return applyEquipment(player, cards);
    }

    /// Atomically lowers an equipped card by one supported rarity, or removes its minimum tier.
    public static DataResult<EquipmentChange> downgradeCard(ServerPlayer player, Card card) {
        var playerData = PolyCard.storage().getPlayerData(player);
        if (playerData.equippedCards.get(card.cardType()) != card.rarityLevel()) {
            return DataResult.error(() -> card.cardType() + " is not equipped at " + card.rarityLevel());
        }

        var target = playerData.getEquippedCards();
        target.remove(card);
        card.previous().ifPresent(target::add);
        return applyEquipment(player, target);
    }

    private static DataResult<EquipmentChange> applyEquipment(ServerPlayer player, Collection<Card> cards) {
        var playerData = PolyCard.storage().getPlayerData(player);
        return playerData.validateEquipment(cards).map(target -> {
            var before = new EnumMap<>(playerData.equippedCards);
            if (before.equals(target)) {
                return EquipmentChange.NONE;
            }

            var unequipped = changedCards(before, target);
            var equipped = changedCards(target, before);
            playerData.equippedCards.clear();
            playerData.equippedCards.putAll(target);

            unequipped.forEach(card -> CardEventCallback.UNEQUIPPED.invoker().onCardUnequip(player, card));
            equipped.forEach(card -> CardEventCallback.EQUIPPED.invoker().onCardEquip(player, card));
            PolyCard.storage().setDirty();
            return new EquipmentChange(unequipped, equipped);
        });
    }

    private DataResult<EnumMap<CardType, RarityLevel>> validateEquipment(Collection<Card> cards) {
        if (cards.size() > MAX_EQUIPPED_CARDS) {
            return DataResult.error(() -> "there can only be " + MAX_EQUIPPED_CARDS + " equipped cards");
        }

        var target = new EnumMap<CardType, RarityLevel>(CardType.class);
        var mutexGroups = new HashMap<String, CardType>();
        for (var card : cards) {
            if (target.put(card.cardType(), card.rarityLevel()) != null) {
                return DataResult.error(() -> card.cardType() + " is equipped more than once");
            }

            var mutexGroup = card.cardType().getMutexGroup();
            if (!mutexGroup.isBlank()) {
                var conflictingType = mutexGroups.put(mutexGroup, card.cardType());
                if (conflictingType != null) {
                    return DataResult.error(() -> "cannot equip " + card.cardType() + " because " + conflictingType + " is in the same " + mutexGroup + " group");
                }
            }
        }
        return DataResult.success(target);
    }

    private static List<Card> changedCards(
            Map<CardType, RarityLevel> source,
            Map<CardType, RarityLevel> comparison
    ) {
        var changed = new ArrayList<Card>();
        source.forEach((cardType, rarityLevel) -> {
            if (comparison.get(cardType) != rarityLevel) {
                changed.add(new Card(cardType, rarityLevel));
            }
        });
        return List.copyOf(changed);
    }

    private static List<Card> cardsFrom(Map<CardType, RarityLevel> cards) {
        var result = new ArrayList<Card>(cards.size());
        cards.forEach((cardType, rarityLevel) -> result.add(new Card(cardType, rarityLevel)));
        return result;
    }

    /// The cards removed and added by one committed equipment transaction.
    public record EquipmentChange(List<Card> unequipped, List<Card> equipped) {
        private static final EquipmentChange NONE = new EquipmentChange(List.of(), List.of());
    }
}
