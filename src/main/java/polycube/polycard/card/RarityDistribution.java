package polycube.polycard.card;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// Immutable, validated probability distribution for one card type.
///
/// Card probabilities are cumulative: each value is the chance of receiving that
/// rarity or a better one. This module is the single place that interprets those
/// thresholds for both direct rolls and generated loot-table weights.
public final class RarityDistribution {
    private final List<Rarity> rarities;
    private final List<WeightedOutcome> weightedOutcomes;

    private RarityDistribution(List<Rarity> rarities, List<WeightedOutcome> weightedOutcomes) {
        this.rarities = rarities;
        this.weightedOutcomes = weightedOutcomes;
    }

    /// Validates cumulative thresholds and derives their exact integer weights.
    public static RarityDistribution of(List<Rarity> configuredRarities) {
        if (configuredRarities.isEmpty()) {
            throw new IllegalArgumentException("A rarity distribution must contain at least one rarity");
        }

        var rarities = List.copyOf(configuredRarities);
        int expectedRank = rarities.getFirst().rarityLevel().rank();
        float previousProbability = 1.0F;
        int scale = 0;

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
                throw new IllegalArgumentException("Cumulative rarity probabilities must be non-increasing");
            }

            previousProbability = rarity.probability();
            scale = Math.max(scale, decimal(rarity.probability()).scale());
        }

        var totalWeight = BigInteger.TEN.pow(scale);
        var outcomes = new ArrayList<WeightedOutcome>();
        for (int i = 0; i < rarities.size(); i++) {
            var rarity = rarities.get(i);
            var cumulativeWeight = weight(rarity.probability(), scale);
            var nextCumulativeWeight = i + 1 < rarities.size()
                    ? weight(rarities.get(i + 1).probability(), scale)
                    : BigInteger.ZERO;
            addPositiveOutcome(outcomes, Optional.of(rarity.rarityLevel()), cumulativeWeight.subtract(nextCumulativeWeight));
        }

        addPositiveOutcome(
                outcomes, Optional.empty(),
                totalWeight.subtract(weight(rarities.getFirst().probability(), scale))
        );
        return new RarityDistribution(rarities, List.copyOf(outcomes));
    }

    /// Maps one [0,1) roll to the highest matching cumulative rarity threshold.
    public Optional<RarityLevel> select(float roll) {
        if (!Float.isFinite(roll) || roll < 0.0F || roll >= 1.0F) {
            throw new IllegalArgumentException("Card roll must be finite and in [0, 1): " + roll);
        }

        RarityLevel selected = null;
        for (var rarity : rarities) {
            if (roll >= rarity.probability()) {
                break;
            }
            selected = rarity.rarityLevel();
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

    private static BigInteger weight(float probability, int scale) {
        return decimal(probability).movePointRight(scale).toBigIntegerExact();
    }

    private static void addPositiveOutcome(List<WeightedOutcome> outcomes, Optional<RarityLevel> rarityLevel, BigInteger weight) {
        if (weight.signum() > 0) {
            outcomes.add(new WeightedOutcome(rarityLevel, weight.intValueExact()));
        }
    }

    /// One mutually exclusive loot outcome; an empty rarity means no card drops.
    public record WeightedOutcome(Optional<RarityLevel> rarityLevel, int weight) {}
}
