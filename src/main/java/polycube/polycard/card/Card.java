package polycube.polycard.card;

import net.minecraft.network.chat.Component;

public record Card(CardType type, Rarity rarity) {
    public Component getFormatedName() { // TODO: Add item tooltip
        return Component.literal(type.getName()).withStyle(rarity.getColor());
    }

    public String getId() {
        return rarity.getId() + "_" + type.getId();
    }

    public static Card fromId(String id) {
        if (id == null) {
            return null;
        }
        String[] parts = id.split("_", 2);
        if (parts.length != 2) {
            return null;
        }
        var rarity = Rarity.fromId(parts[0]);
        var type = CardType.fromId(parts[1]);
        if (rarity.isEmpty() || type.isEmpty()) {
            return null;
        }
        return new Card(type.get(), rarity.get());
    }
}
