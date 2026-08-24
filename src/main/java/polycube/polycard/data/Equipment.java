package polycube.polycard.data;

import com.mojang.serialization.DataResult;
import org.jspecify.annotations.Nullable;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;

import java.util.*;

/// Immutable, validated set of cards currently active for one player.
public final class Equipment {
    public static final int MAX_CARDS = 5;

    private final EnumMap<CardType, RarityLevel> cardsByType;

    private Equipment(Map<CardType, RarityLevel> cardsByType) {
        this.cardsByType = new EnumMap<>(cardsByType);
    }

    /// Strictly validates a complete proposed equipment set.
    public static DataResult<Equipment> create(Collection<Card> cards) {
        if (cards.size() > MAX_CARDS) {
            return DataResult.error(() -> "there can only be " + MAX_CARDS + " equipped cards");
        }

        var target = new EnumMap<CardType, RarityLevel>(CardType.class);
        var mutexGroups = new HashMap<String, CardType>();
        for (var card : cards) {
            if (target.putIfAbsent(card.cardType(), card.rarityLevel()) != null) {
                return DataResult.error(() -> card.cardType() + " is equipped more than once");
            }

            var mutexGroup = card.cardType().getMutexGroup();
            if (!mutexGroup.isBlank()) {
                var conflictingType = mutexGroups.putIfAbsent(mutexGroup, card.cardType());
                if (conflictingType != null) {
                    return DataResult.error(() -> "cannot equip " + card.cardType() + " because " + conflictingType + " is in the same " + mutexGroup + " group");
                }
            }
        }
        return DataResult.success(new Equipment(target));
    }

    /// Repairs untrusted persisted data deterministically and reports every discarded entry.
    public static EquipmentRepair repair(Map<CardType, RarityLevel> persistedCards) {
        var repaired = new EnumMap<CardType, RarityLevel>(CardType.class);
        var mutexGroups = new HashSet<String>();
        var discarded = new EnumMap<CardType, RarityLevel>(CardType.class);

        for (var cardType : CardType.values()) {
            var rarityLevel = persistedCards.get(cardType);
            if (rarityLevel == null) continue;
            if (repaired.size() == MAX_CARDS || Card.tryCreate(cardType, rarityLevel).isEmpty()) {
                discarded.put(cardType, rarityLevel);
                continue;
            }

            var mutexGroup = cardType.getMutexGroup();
            if (!mutexGroup.isBlank() && !mutexGroups.add(mutexGroup)) {
                discarded.put(cardType, rarityLevel);
                continue;
            }

            repaired.put(cardType, rarityLevel);
        }

        return new EquipmentRepair(new Equipment(repaired), discarded);
    }

    /// Equips a card, atomically replacing another rarity of its type or mutex-group peer.
    public DataResult<Equipment> equipOrReplace(Card card) {
        var target = new ArrayList<>(cards());
        var mutexGroup = card.cardType().getMutexGroup();
        var replacedCard = target.stream()
                .filter(equippedCard -> equippedCard.cardType() == card.cardType()
                        || (!mutexGroup.isBlank()
                        && equippedCard.cardType().getMutexGroup().equals(mutexGroup)))
                .findFirst();

        if (replacedCard.filter(card::equals).isPresent()) {
            return DataResult.error(() -> card.cardType() + " is already equipped");
        }
        replacedCard.ifPresent(target::remove);
        target.add(card);
        return create(target);
    }

    /// Lowers an equipped card by one supported rarity, removing its minimum tier.
    public DataResult<Equipment> downgrade(Card card) {
        if (cardsByType.get(card.cardType()) != card.rarityLevel()) {
            return DataResult.error(() -> card.cardType() + " is not equipped at " + card.rarityLevel());
        }

        var target = new ArrayList<>(cards());
        target.remove(card);
        card.previous().ifPresent(target::add);
        return create(target);
    }

    /// Returns equipped cards in stable CardType order.
    public List<Card> cards() {
        var cards = new ArrayList<Card>(cardsByType.size());
        cardsByType.forEach((cardType, rarityLevel) -> Card.tryCreate(cardType, rarityLevel).ifPresent(cards::add));
        return List.copyOf(cards);
    }

    public Map<CardType, RarityLevel> cardsByType() {
        return Collections.unmodifiableMap(cardsByType);
    }

    public @Nullable RarityLevel rarityLevel(CardType cardType) {
        return cardsByType.get(cardType);
    }

    public int size() {
        return cardsByType.size();
    }

    public boolean contains(CardType cardType, RarityLevel rarityLevel) {
        return cardsByType.get(cardType) == rarityLevel;
    }

    public boolean containsAtLeast(CardType cardType, RarityLevel rarityLevel) {
        return Optional.ofNullable(cardsByType.get(cardType))
                .filter(equippedRarity -> equippedRarity.isAtLeast(rarityLevel))
                .isPresent();
    }

    public boolean containsType(CardType cardType) {
        return cardsByType.containsKey(cardType);
    }

    @Override
    public boolean equals(Object object) {
        return this == object
                || object instanceof Equipment equipment
                && cardsByType.equals(equipment.cardsByType);
    }

    @Override
    public int hashCode() {
        return cardsByType.hashCode();
    }

    public record EquipmentRepair(Equipment repaired, Map<CardType, RarityLevel> discarded) {}
}
