package polycube.polycard.card;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/// Represents the rarity of a card, including its rarity level, probability of occurrence,
/// whether it is enchanted, and a description of its effect.
public record Rarity(
        RarityLevel rarityLevel, double probability, boolean isEnchanted, String description,
        Multimap<Holder<Attribute>, AttributeModifier> attributeModifiers
) {
    public Component getFormattedDescription() {
        return Component.literal(description).withStyle(rarityLevel.color());
    }

    public Rarity withAttribute(Holder<Attribute> attribute, AttributeModifier modifier) {
        attributeModifiers.put(attribute, modifier);
        return this;
    }
}
