package polycube.polycard.card;

import net.minecraft.network.chat.Component;

public record Rarity(RarityType type, int probability, boolean isEnchanted, String description) {
    public Component getFormattedDescription() {
        return Component.literal(description).withStyle(type.color());
    }
}
