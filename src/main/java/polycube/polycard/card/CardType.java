package polycube.polycard.card;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jspecify.annotations.NonNull;
import polycube.polycard.cardEffects.neutral.EnderManEffects;
import polycube.polycard.cardEffects.neutral.IronGolemEffects;
import polycube.polycard.cardEffects.passive.CowEffects;
import polycube.polycard.cardEffects.passive.SquidEffects;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/// Represents the type of a card, which determines the conditions for obtaining the card and its potential effects based on rarity levels.
public enum CardType implements StringRepresentable {
    // Passive

    COW("breeding two cows", "passive") {{
        addRarity(RarityLevel.UNCOMMON, 0.1, false, "Regeneration when standing in plains");
        addRarity(RarityLevel.RARE, 0.05, false, "Gain resistance when near other Cow Card");
        addRarity(RarityLevel.EPIC, 0.01, true, "Convert debuffs into Buffs when drinking milk");
        addRarity(RarityLevel.LEGENDARY, 0.001, true, "+" + CowEffects.REGEN_HEALTH_GAIN + " hearts when drinking milk");
    }},
    SQUID("killing a squid", "passive") {{
        addRarity(RarityLevel.RARE, 0.05, false, SquidEffects.BLINDNESS_WHEN_HIT_CHANCE + "% chance to give blindness when hit");
        addRarity(RarityLevel.EPIC, 0.01, true, SquidEffects.BLINDNESS_ON_HIT_CHANCE + "% chance to give blindness on hit");
        addRarity(RarityLevel.LEGENDARY, 0.001, true, "Water breathing");
    }},
    BEE("collecting honey", "passive") {{
        addRarity(RarityLevel.RARE, 0.05, false, "Drinking honey gives speed");
        addRarity(RarityLevel.EPIC, 0.01, true, "Drinking honey gives regeneration");
        addRarity(RarityLevel.LEGENDARY, 0.001, true, "Drinking honey gives health boost");
    }},

    // Neutral

    IRON_GOLEM("summoning an Iron Golem", "neutral") {{
        addRarity(RarityLevel.RARE, 0.05, false, IronGolemEffects.RESISTANCE_ON_ATTACKED_CHANCE + "% chance to gain resistance when attacked");
        addRarity(RarityLevel.EPIC, 0.01, true, "Hitting with fist knock back enemies (10 sec cooldown)");
        addRarity(RarityLevel.LEGENDARY, 0.001, true, "Falling creates shock wave (10 sec cooldown)");
    }},
    ENDERMAN("killing an Enderman", "neutral") {{
        addRarity(RarityLevel.UNCOMMON, 0.1, false, "No ender pearl damage");
        addRarity(RarityLevel.RARE, 0.05, false, "No ender pearl cooldown");
        addRarity(RarityLevel.EPIC, 0.01, true, EnderManEffects.PROJECTILE_DODGE_CHANCE + "% chance to dodge projectile");
        addRarity(RarityLevel.LEGENDARY, 0.001, true, "Resistance in the End");
    }},
    PIGLIN("killing a piglin", "neutral") {{
        addRarity(RarityLevel.UNCOMMON, 0.1, false, "Piglins do not attack you");
        addRarity(RarityLevel.RARE, 0.05, false, "Gold food gives a random buff when eaten");
        addRarity(RarityLevel.EPIC, 0.01, true, "Piglin brutes do not attack you");
        addRarity(RarityLevel.LEGENDARY, 0.001, true, "Gold items are significantly more durable");
    }},

    // Hostile

    ZOMBIE("killing a zombie", "hostile") {{
        addRarity(RarityLevel.COMMON, 0.25, false, "No hunger when eating rotten flesh");
        addRarity(RarityLevel.UNCOMMON, 0.1, false, "Rotten flesh gives +2 food");
        addRarity(RarityLevel.RARE, 0.05, false, "Rotten flesh gives +2 food");
        addRarity(RarityLevel.EPIC, 0.01, true, "Rotten flesh gives strength");
        addRarity(RarityLevel.LEGENDARY, 0.001, true, "Rotten flesh gives regeneration");
    }};

    public static final Codec<CardType> CODEC = StringRepresentable.fromValues(CardType::values);
    public static final Map<String, CardType> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(CardType::getSerializedName, Function.identity()));

    private final Map<RarityLevel, Rarity> rarities = new HashMap<>();
    private final String condition;
    private final String cardGroup;

    CardType(String condition, String cardGroup) {
        this.condition = condition;
        this.cardGroup = cardGroup;
    }

    /// Adds a rarity level to this card type with the specified properties.
    ///
    /// @param rarityLevel The rarity level to add.
    /// @param probability The probability of obtaining this card at the specified rarity level.
    /// @param isEnchanted Whether the card should have an enchanted appearance at this rarity level.
    /// @param description A description of the effects or properties of the card at this rarity level.
    /// @return The created Rarity object representing the added rarity level.
    protected Rarity addRarity(RarityLevel rarityLevel, double probability, boolean isEnchanted, String description) {
        var rarity = new Rarity(rarityLevel, probability, isEnchanted, description, HashMultimap.create());
        rarities.put(rarityLevel, rarity);
        return rarity;
    }

    /// Gets the minimum rarity level available for this card.
    ///
    /// @return The minimum rarity level available for this card.
    public RarityLevel minRarityLevel() {
        return rarities.keySet().stream().min(Comparator.comparingInt(Enum::ordinal)).orElseThrow();
    }

    /// Gets a list of all rarities available for this card, sorted by their rarity level.
    ///
    /// @return A list of rarities for this card, sorted by rarity level.
    public List<Rarity> getRarities() {
        return rarities.values().stream()
                .sorted(Comparator.comparing(r -> r.rarityLevel().ordinal()))
                .toList();
    }

    /// Gets a multimap of attribute modifiers for this card type, including all modifiers from rarities up to the specified maximum rarity level.
    ///
    /// @param maxRarity The maximum rarity level to include when gathering attribute modifiers.
    /// @return A multimap of attribute modifiers for this card type, including all modifiers from rarities up to the specified maximum rarity level.
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(RarityLevel maxRarity) {
        Multimap<Holder<Attribute>, AttributeModifier> attributes = HashMultimap.create();
        for (var rarity : rarities.keySet().stream().sorted(Comparator.comparing(Enum::ordinal)).toList()) {
            var typeAttributes = rarities.get(rarity).attributeModifiers();
            if (typeAttributes != null) {
                attributes.putAll(typeAttributes);
            }
            if (rarity == maxRarity) break;
        }
        return attributes;
    }

    /// Gets the condition for obtaining this card type, which describes how a player can acquire the card.
    ///
    /// @return The condition for obtaining this card type.
    public String getCondition() {
        return condition;
    }

    /// Gets the card group for this card type, which categorizes the card into a specific group for organizational purposes.
    ///
    /// @return The card group for this card type.
    public String getCardGroup() {
        return cardGroup;
    }

    /// Gets the unique identifier for this card type, which is a combination of the card group and the serialized name of the card type.
    ///
    /// @return The unique identifier for this card type, formatted as "cardGroup/serializedName".
    public String getId() {
        return getCardGroup() + "/" + getSerializedName();
    }

    /// Deserializes a CardType from a string.
    ///
    /// @param string the string to deserialize
    /// @return an Optional containing the deserialized CardType, or empty if the string is invalid
    public static Optional<CardType> deserialize(String string) {
        return Optional.ofNullable(BY_NAME.get(string));
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.name().toLowerCase();
    }

    @Override
    public String toString() {
        var words = getSerializedName().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(word.substring(0, 1).toUpperCase()).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
