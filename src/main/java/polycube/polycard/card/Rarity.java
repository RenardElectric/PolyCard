package polycube.polycard.card;

import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jspecify.annotations.Nullable;

/// Configuration for one supported rarity tier of a card type.
public record Rarity(
        RarityLevel rarityLevel, float probability, boolean isEnchanted, String description,
        Multimap<@Nullable Holder<Attribute>, @Nullable AttributeModifier> attributeModifiers
) {
    public Component getFormattedDescription() {
        return Component.literal(description).withStyle(rarityLevel.color());
    }

    /// Adds a modifier applied while a card of this rarity or higher is equipped.
    @SuppressWarnings("unused")
    public Rarity withAttribute(Holder<Attribute> attribute, AttributeModifier modifier) {
        attributeModifiers.put(attribute, modifier);
        return this;
    }
}
