package polycube.polycard.card;

import com.mojang.serialization.DataResult;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import polycube.polycard.events.callBacks.CardProbabilityOverrideCallback;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/// Immutable, validated probability distribution for one card type.
///
/// Each rarity uses an independent roll. A rarity replaces any earlier selection
/// when its roll is less than its configured probability.
public final class RarityDistribution {
    private final List<Rarity> rarities;
    private final EnumMap<RarityLevel, Rarity> byRarity;

    private RarityDistribution(List<Rarity> rarities) {
        this.rarities = rarities;
        byRarity = new EnumMap<>(RarityLevel.class);
        rarities.forEach(rarity -> byRarity.put(rarity.rarityLevel(), rarity));
    }

    /// Creates an immutable distribution, rejecting invalid definitions immediately.
    public static RarityDistribution of(List<Rarity> rarities) {
        return create(rarities).getOrThrow(IllegalArgumentException::new);
    }

    /// Reports invalid user-supplied probabilities through the codec result interface.
    public static DataResult<RarityDistribution> create(List<Rarity> rarities) {
        var snapshot = List.copyOf(rarities);
        try {
            checkRarities(snapshot);
            return DataResult.success(new RarityDistribution(snapshot));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }

    /// Overlays probabilities on a card's supported tiers without changing its effects.
    public static DataResult<RarityDistribution> withProbabilities(CardType type, Map<RarityLevel, Float> probabilities) {
        for (var rarity : probabilities.keySet()) {
            if (!type.hasRarity(rarity)) {
                return DataResult.error(() -> type.getSerializedName() + " does not support " + rarity.getSerializedName());
            }
        }
        return create(type.definition().raritiesList().stream()
                .map(rarity -> new Rarity(
                        rarity.rarityLevel(),
                        probabilities.getOrDefault(rarity.rarityLevel(), rarity.probability()),
                        rarity.isEnchanted(),
                        rarity.description()
                ))
                .toList());
    }

    /// Validates that the rarity list is non-empty, contiguous, and has valid probabilities.
    /// A zero probability disables that tier's drops and is ignored by the non-increasing rule.
    private static void checkRarities(List<Rarity> rarities) {
        if (rarities.isEmpty()) {
            throw new IllegalArgumentException("A rarity distribution must contain at least one rarity");
        }
        int expectedRank = rarities.getFirst().rarityLevel().rank();
        float previousProbability = 1.0F;
        for (var rarity : rarities) {
            if (rarity.rarityLevel().rank() != expectedRank++) {
                throw new IllegalArgumentException("Rarity tiers must be contiguous from the minimum rarity");
            }
            if (!Float.isFinite(rarity.probability())
                    || rarity.probability() < 0.0F
                    || rarity.probability() > 1.0F) {
                throw new IllegalArgumentException("Rarity probability must be finite and in [0, 1]: " + rarity);
            }
            if (rarity.probability() == 0.0F) {
                continue;
            }
            if (rarity.probability() > previousProbability) {
                throw new IllegalArgumentException("Rarity probabilities must be non-increasing");
            }
            previousProbability = rarity.probability();
        }
    }

    /// Independently rolls every rarity and returns the last one whose roll meets its threshold.
    public Optional<RarityLevel> select(CardType cardType, @Nullable ServerPlayer player, Random random) {
        RarityLevel selected = null;
        for (var rarity : rarities) {
            var proba = rarity.probability();
            if (player != null) proba = CardProbabilityOverrideCallback.EVENT.invoker().cardProbabilityOverride(player, cardType, rarity.rarityLevel(), proba);

            if (random.nextFloat() < proba) {
                selected = rarity.rarityLevel();
            }
        }
        return Optional.ofNullable(selected);
    }

    public List<Rarity> rarities() {
        return rarities;
    }

    public Optional<Rarity> rarity(RarityLevel rarityLevel) {
        return Optional.ofNullable(byRarity.get(rarityLevel));
    }

    public Map<RarityLevel, Float> probabilities() {
        var probabilities = new EnumMap<RarityLevel, Float>(RarityLevel.class);
        rarities.forEach(rarity -> probabilities.put(rarity.rarityLevel(), rarity.probability()));
        return Collections.unmodifiableMap(probabilities);
    }
}
