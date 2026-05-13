package polycube.polycard.card;

import net.minecraft.network.chat.Component;

/// Represents the rarity of a card, including its rarity level, probability of occurrence,
/// whether it is enchanted, and a description of its effect.
public record Rarity(RarityLevel rarityLevel, int probability, boolean isEnchanted, String description) {
    public Component getFormattedDescription() {
        return Component.literal(description).withStyle(rarityLevel.color());
    }
}
