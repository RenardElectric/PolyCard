package polycube.polycard.card;

import net.minecraft.resources.Identifier;
import polycube.polycard.cardEffects.CardEffects;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/// Immutable authoritative definition of one card type.
public record CardDefinition(
        String serializedName, String acquisitionCondition, CardGroup group,
        String mutexGroup, RarityDistribution rarityDistribution,
        EnumMap<RarityLevel, Rarity> rarities, Supplier<CardEffects> effectFactory
) {
    private static CardDefinition from(CardDefinitionBuilder builder) {
        var serializedName = requireText(builder.serializedName, "serializedName");
        var acquisitionCondition = requireText(builder.acquisitionCondition, "acquisitionCondition");
        var group = builder.group;
        var mutexGroup = builder.mutexGroup.strip();
        var rarityDistribution = RarityDistribution.of(List.copyOf(builder.rarities.values()));
        var effectFactory = builder.effectFactory;

        var rarities = new EnumMap<RarityLevel, Rarity>(RarityLevel.class);
        for (var rarity : rarityDistribution.rarities()) {
            rarities.put(rarity.rarityLevel(), rarity);
        }
        return new CardDefinition(serializedName, acquisitionCondition, group, mutexGroup, rarityDistribution, rarities, effectFactory);
    }

    /// Starts a builder for one immutable card definition.
    public static CardDefinitionBuilder builder(
            String serializedName, String acquisitionCondition,
            CardGroup group, Supplier<CardEffects> effectFactory
    ) {
        return new CardDefinitionBuilder(serializedName, acquisitionCondition, group, effectFactory);
    }

    public Identifier id() {
        return group.getId().withSuffix("/" + serializedName);
    }

    public RarityLevel minRarityLevel() {
        return rarityDistribution.rarities().getFirst().rarityLevel();
    }

    public Optional<Rarity> rarity(RarityLevel rarityLevel) {
        return Optional.ofNullable(rarities.get(rarityLevel));
    }

    public boolean supports(RarityLevel rarityLevel) {
        return rarities.containsKey(rarityLevel);
    }

    public List<Rarity> raritiesList() {
        return rarityDistribution.rarities();
    }

    public RarityDistribution rarityDistribution() {
        return rarityDistribution;
    }

    CardEffects createEffects() {
        return effectFactory.get();
    }

    /// Mutable construction interface for a [CardDefinition].
    public static final class CardDefinitionBuilder {
        private final String serializedName;
        private final String acquisitionCondition;
        private final CardGroup group;
        private final Supplier<CardEffects> effectFactory;
        private final EnumMap<RarityLevel, Rarity> rarities = new EnumMap<>(RarityLevel.class);
        private String mutexGroup = "";

        private CardDefinitionBuilder(
                String serializedName, String acquisitionCondition,
                CardGroup group, Supplier<CardEffects> effectFactory
        ) {
            this.serializedName = serializedName;
            this.acquisitionCondition = acquisitionCondition;
            this.group = group;
            this.effectFactory = effectFactory;
        }

        public CardDefinitionBuilder mutexGroup(String mutexGroup) {
            this.mutexGroup = mutexGroup;
            return this;
        }

        public CardDefinitionBuilder addRarity(
                RarityLevel rarityLevel, float probability,
                boolean isEnchanted, String description
        ) {
            var rarity = new Rarity(rarityLevel, probability, isEnchanted, description);
            var previous = rarities.putIfAbsent(rarityLevel, rarity);
            if (previous != null) throw new IllegalStateException(serializedName + " defines " + rarityLevel + " more than once");
            return this;
        }

        public CardDefinition build() {
            return CardDefinition.from(this);
        }
    }

    private static String requireText(String value, String name) {
        var stripped = value.strip();
        if (stripped.isEmpty()) throw new IllegalArgumentException(name + " cannot be blank");
        return stripped;
    }
}
