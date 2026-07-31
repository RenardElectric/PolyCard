package polycube.polycard.card;

import net.minecraft.network.chat.Component;

/// Configuration for one supported rarity tier of a card type.
public record Rarity(
        RarityLevel rarityLevel, float probability, boolean isEnchanted, String description
) {
    public Component getFormattedDescription() {
        return Component.literal(description).withStyle(rarityLevel.color());
    }
}
