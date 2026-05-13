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

/// Represents the rarity level of a card, including its name and associated color for display purposes.
public enum RarityLevel implements StringRepresentable {
    COMMON("common", ChatFormatting.GRAY),
    UNCOMMON("uncommon", ChatFormatting.GREEN),
    RARE("rare", ChatFormatting.BLUE),
    EPIC("epic", ChatFormatting.DARK_PURPLE),
    LEGENDARY("legendary", ChatFormatting.GOLD);

    public static final Codec<RarityLevel> CODEC = StringRepresentable.fromValues(RarityLevel::values);
    public static final Map<String, RarityLevel> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(rarity -> rarity.name, Function.identity()));

    private final String name;
    private final ChatFormatting color;

    RarityLevel(String name, ChatFormatting color) {
        this.name = name;
        this.color = color;
    }

    /// Gets the next rarity level in the progression, if it exists.
    ///
    /// @return An Optional containing the next RarityLevel if it exists,
    /// or an empty Optional if this is the highest rarity level (LEGENDARY).
    public Optional<RarityLevel> nextLevel() {
        return switch (this) {
            case COMMON -> Optional.of(UNCOMMON);
            case UNCOMMON -> Optional.of(RARE);
            case RARE -> Optional.of(EPIC);
            case EPIC -> Optional.of(LEGENDARY);
            case LEGENDARY -> Optional.empty(); // No next level after LEGENDARY
        };
    }

    /// Gets the color associated with this rarity level for display purposes.
    ///
    /// @return The ChatFormatting color associated with this rarity level.
    public ChatFormatting color() {
        return this.color;
    }

    /// Deserializes a string into a RarityLevel.
    ///
    /// @param string The string representation of the rarity level to deserialize.
    /// @return An Optional containing the corresponding RarityLevel if the string is valid,
    ///         or an empty Optional if the string does not match any RarityLevel.
    public static Optional<RarityLevel> deserialize(String string) {
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
