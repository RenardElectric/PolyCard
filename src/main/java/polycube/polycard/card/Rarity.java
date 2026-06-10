package polycube.polycard.card;

import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/// Represents the rarity of a card, including its rarity level, probability of occurrence,
/// whether it is enchanted, and a description of its effect.
public record Rarity(
        RarityLevel rarityLevel, float probability, boolean isEnchanted, String description,
        Multimap<Holder<Attribute>, AttributeModifier> attributeModifiers
) {
    public Component getFormattedDescription() {
        return Component.literal(description).withStyle(rarityLevel.color());
    }

    /// Adds an attribute modifier to this rarity, which modifies a specific attribute when the card is used.
    ///
    /// @param attribute The attribute to modify, represented as a Holder of an Attribute.
    /// @param modifier  The AttributeModifier that defines how the attribute should be modified.
    /// @return The Rarity instance with the added attribute modifier, allowing for method chaining.
    public Rarity withAttribute(Holder<Attribute> attribute, AttributeModifier modifier) {
        attributeModifiers.put(attribute, modifier);
        return this;
    }
}
