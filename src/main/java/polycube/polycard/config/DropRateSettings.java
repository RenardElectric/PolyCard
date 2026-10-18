package polycube.polycard.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityDistribution;
import polycube.polycard.card.RarityLevel;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/// Immutable drop rates decoded entirely through codecs. Missing entries use code defaults.
public record DropRateSettings(Map<CardType, RarityDistribution> distributions) {
    private static final Codec<CardType> CARD_TYPE_CODEC = Codec.STRING.comapFlatMap(
            id -> CardType.deserialize(id).map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown card: " + id)),
            CardType::getSerializedName);
    private static final Codec<RarityLevel> RARITY_CODEC = Codec.STRING.comapFlatMap(
            id -> RarityLevel.deserialize(id).map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown rarity: " + id)),
            RarityLevel::getSerializedName);

    public static final Codec<DropRateSettings> CODEC = Codec.dispatchedMap(CARD_TYPE_CODEC, DropRateSettings::cardRatesCodec)
            .optionalFieldOf("drop_rates", Map.of())
            .xmap(DropRateSettings::new, DropRateSettings::distributions).codec();

    public DropRateSettings {
        var copy = new EnumMap<CardType, RarityDistribution>(CardType.class);
        copy.putAll(distributions);
        distributions = Collections.unmodifiableMap(copy);
    }

    public static DropRateSettings defaults() {
        var defaults = new EnumMap<CardType, RarityDistribution>(CardType.class);
        for (var type : CardType.values()) {
            defaults.put(type, type.definition().rarityDistribution());
        }
        return new DropRateSettings(defaults);
    }

    public RarityDistribution distribution(CardType type) {
        var distribution = distributions.get(type);
        return distribution != null ? distribution : type.definition().rarityDistribution();
    }

    private static Codec<RarityDistribution> cardRatesCodec(CardType type) {
        return Codec.unboundedMap(RARITY_CODEC, Codec.floatRange(0.0F, 1.0F))
                .comapFlatMap(rates -> RarityDistribution.withProbabilities(type, rates), RarityDistribution::probabilities);
    }
}
