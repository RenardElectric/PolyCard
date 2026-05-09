package polycube.polycard.card;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Rarity implements StringRepresentable {
    COMMON("common", ChatFormatting.GRAY, false, Items.GRAY_DYE),
    UNCOMMON("uncommon", ChatFormatting.GREEN, false, Items.GREEN_DYE),
    RARE("rare", ChatFormatting.BLUE, false, Items.BLUE_DYE),
    EPIC("epic", ChatFormatting.DARK_PURPLE, true, Items.PURPLE_DYE),
    LEGENDARY("legendary", ChatFormatting.GOLD, true, Items.ORANGE_DYE);

    public static final Codec<Rarity> CODEC = StringRepresentable.fromValues(Rarity::values);
    public static final Map<String, Rarity> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(rarity -> rarity.name, Function.identity()));

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

    public ChatFormatting color() {
        return this.color;
    }

    public boolean isEnchanted() {
        return isEnchanted;
    }

    public Item item() {
        return item;
    }

    public static Optional<Rarity> deserialize(String string) {
        return Optional.ofNullable(BY_NAME.get(string));
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.name;
    }

    @Override
    public String toString() {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
