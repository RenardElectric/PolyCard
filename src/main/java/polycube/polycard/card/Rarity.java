package polycube.polycard.card;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum Rarity {
    COMMON("common", ChatFormatting.GRAY, false, Items.GRAY_DYE),
    UNCOMMON("uncommon", ChatFormatting.GREEN, false, Items.GREEN_DYE),
    RARE("rare", ChatFormatting.BLUE, false, Items.BLUE_DYE),
    EPIC("epic", ChatFormatting.DARK_PURPLE, true, Items.PURPLE_DYE),
    LEGENDARY("legendary", ChatFormatting.GOLD, true, Items.ORANGE_DYE),;

    private final String name;
    private final ChatFormatting color;
    private final boolean isEnchanted;
    private final Item item;

    Rarity(final String name, final ChatFormatting color, final boolean isEnchanted, final Item item) {
        this.name = name;
        this.color = color;
        this.isEnchanted = isEnchanted;
        this.item = item;
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
}
