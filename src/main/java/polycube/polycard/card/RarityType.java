package polycube.polycard.card;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum RarityType implements StringRepresentable {
    COMMON("common", ChatFormatting.GRAY),
    UNCOMMON("uncommon", ChatFormatting.GREEN),
    RARE("rare", ChatFormatting.BLUE),
    EPIC("epic", ChatFormatting.DARK_PURPLE),
    LEGENDARY("legendary", ChatFormatting.GOLD);

    public static final Codec<RarityType> CODEC = StringRepresentable.fromValues(RarityType::values);
    public static final Map<String, RarityType> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(rarity -> rarity.name, Function.identity()));

    private final String name;
    private final ChatFormatting color;

    RarityType(String name, ChatFormatting color) {
        this.name = name;
        this.color = color;
    }

    public ChatFormatting color() {
        return this.color;
    }

    public static Optional<RarityType> deserialize(String string) {
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
