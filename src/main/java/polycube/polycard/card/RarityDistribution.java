package polycube.polycard.card;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/// Immutable, validated probability distribution for one card type.
///
/// Each rarity uses an independent roll. A rarity replaces any earlier selection
/// when its roll is less than its configured probability. This module derives
/// the equivalent mutually exclusive loot-table weights.
public final class RarityDistribution {
    private final List<Rarity> rarities;
    private final List<WeightedOutcome> weightedOutcomes;

    private RarityDistribution(List<Rarity> rarities, List<WeightedOutcome> weightedOutcomes) {
        this.rarities = rarities;
        this.weightedOutcomes = weightedOutcomes;
    }

    /// Validates the configured thresholds and derives equivalent integer weights.
    public static RarityDistribution of(List<Rarity> rarities) {
        checkRarities(rarities);

        var chances = new ArrayList<BigDecimal>();
        var noLaterRarity = BigDecimal.ONE;
        for (int i = rarities.size() - 1; i >= 0; i--) {
            var probability = decimal(rarities.get(i).probability());
            chances.addFirst(probability.multiply(noLaterRarity));
            noLaterRarity = noLaterRarity.multiply(BigDecimal.ONE.subtract(probability));
        }
        chances.add(noLaterRarity);

        int scale = Math.min(9, chances.stream().mapToInt(BigDecimal::scale).max().orElseThrow());
        var weights = chances.stream()
                .mapToInt(chance -> chance.movePointRight(scale).setScale(0, RoundingMode.HALF_UP).intValueExact())
                .toArray();

        var outcomes = new ArrayList<WeightedOutcome>();
        for (int i = 0; i < rarities.size(); i++) {
            addPositiveOutcome(outcomes, rarities.get(i).rarityLevel(), weights[i]);
        }
        addPositiveOutcome(outcomes, null, weights[weights.length - 1]);

        return new RarityDistribution(rarities, List.copyOf(outcomes));
    }

    /// Validates that the rarity list is non-empty, contiguous, and has valid probabilities.
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
                    || rarity.probability() <= 0.0F
                    || rarity.probability() > 1.0F) {
                throw new IllegalArgumentException("Rarity probability must be finite and in (0, 1]: " + rarity);
            }
            if (rarity.probability() > previousProbability) {
                throw new IllegalArgumentException("Rarity probabilities must be non-increasing");
            }
            previousProbability = rarity.probability();
        }
    }

    /// Independently rolls every rarity and returns the last one whose roll meets its threshold.
    public Optional<RarityLevel> select(Random random) {
        RarityLevel selected = null;
        for (var rarity : rarities) {
            if (random.nextFloat() < rarity.probability()) {
                selected = rarity.rarityLevel();
            }
        }
        return Optional.ofNullable(selected);
    }

    public List<Rarity> rarities() {
        return rarities;
    }

    public List<WeightedOutcome> weightedOutcomes() {
        return weightedOutcomes;
    }

    private static BigDecimal decimal(float probability) {
        return new BigDecimal(Float.toString(probability)).stripTrailingZeros();
    }

    private static void addPositiveOutcome(List<WeightedOutcome> outcomes, @Nullable RarityLevel rarityLevel, int weight) {
        if (weight > 0) {
            outcomes.add(new WeightedOutcome(Optional.ofNullable(rarityLevel), weight));
        }
    }

    /// One mutually exclusive loot outcome; an empty rarity means no card drops.
    public record WeightedOutcome(Optional<RarityLevel> rarityLevel, int weight) {}
}
