package polycube.polycard.card;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/// Represents the rarity level of a card, including its name and associated color for display purposes.
public enum RarityLevel implements StringRepresentable {
    COMMON(ChatFormatting.GRAY),
    UNCOMMON(ChatFormatting.GREEN),
    RARE(ChatFormatting.BLUE),
    EPIC(ChatFormatting.DARK_PURPLE),
    LEGENDARY(ChatFormatting.GOLD);

    public static final Codec<RarityLevel> CODEC = StringRepresentable.fromValues(RarityLevel::values);
    public static final Map<String, RarityLevel> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(RarityLevel::getSerializedName, Function.identity()));

    private final ChatFormatting color;

    RarityLevel(ChatFormatting color) {
        this.color = color;
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
        if (string == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_NAME.get(string.toLowerCase(Locale.ROOT)));
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return getSerializedName().substring(0, 1).toUpperCase(Locale.ROOT) + getSerializedName().substring(1);
    }
}
