package polycube.polycard.card;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum Rarity {
    COMMON("common", "Common", ChatFormatting.GRAY, false, Items.GRAY_DYE),
    UNCOMMON("uncommon", "Uncommon", ChatFormatting.GREEN, false, Items.GREEN_DYE),
    RARE("rare", "Rare", ChatFormatting.BLUE, false, Items.BLUE_DYE),
    EPIC("epic", "Epic", ChatFormatting.DARK_PURPLE, true, Items.PURPLE_DYE),
    LEGENDARY("legendary", "Legendary", ChatFormatting.GOLD, true, Items.ORANGE_DYE),;

    private static final Map<String, Rarity> BY_ID;

    private final String id;
    private final String name;
    private final ChatFormatting color;
    private final boolean isEnchanted;
    private final Item item;

    static {
        Map<String, Rarity> byId = new HashMap<>();
        for (Rarity rarity : values()) {
            byId.put(rarity.id, rarity);
        }
        BY_ID = Collections.unmodifiableMap(byId);
    }

    Rarity(final String id, final String name, final ChatFormatting color, final boolean isEnchanted, final Item item) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.isEnchanted = isEnchanted;
        this.item = item;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public boolean isEnchanted() {
        return isEnchanted;
    }

    public Item getItem() {
        return item;
    }

    public static Optional<Rarity> fromId(String id) {
        return Optional.ofNullable(BY_ID.get(id.toLowerCase()));
    }
}
