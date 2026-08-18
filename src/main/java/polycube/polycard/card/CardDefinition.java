package polycube.polycard.card;

import net.minecraft.resources.Identifier;
import polycube.polycard.cardEffects.CardEffects;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/// Immutable authoritative definition of one card type.
public final class CardDefinition {
    private final String serializedName;
    private final String acquisitionCondition;
    private final CardGroup group;
    private final String mutexGroup;
    private final RarityDistribution rarityDistribution;
    private final EnumMap<RarityLevel, Rarity> rarities;
    private final Supplier<CardEffects> effectFactory;

    private CardDefinition(CardDefinitionBuilder builder) {
        serializedName = requireText(builder.serializedName, "serializedName");
        acquisitionCondition = requireText(builder.acquisitionCondition, "acquisitionCondition");
        group = builder.group;
        mutexGroup = builder.mutexGroup.strip();
        rarityDistribution = RarityDistribution.of(List.copyOf(builder.rarities.values()));
        effectFactory = builder.effectFactory;

        rarities = new EnumMap<>(RarityLevel.class);
        for (var rarity : rarityDistribution.rarities()) {
            rarities.put(rarity.rarityLevel(), rarity);
        }
    }

    /// Starts a builder for one immutable card definition.
    public static CardDefinitionBuilder builder(
            String serializedName, String acquisitionCondition,
            CardGroup group, Supplier<CardEffects> effectFactory
    ) {
        return new CardDefinitionBuilder(serializedName, acquisitionCondition, group, effectFactory);
    }

    public String serializedName() {
        return serializedName;
    }

    public String acquisitionCondition() {
        return acquisitionCondition;
    }

    public CardGroup group() {
        return group;
    }

    public String mutexGroup() {
        return mutexGroup;
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

    public List<Rarity> rarities() {
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
            return new CardDefinition(this);
        }
    }

    private static String requireText(String value, String name) {
        var stripped = value.strip();
        if (stripped.isEmpty()) throw new IllegalArgumentException(name + " cannot be blank");
        return stripped;
    }
}
