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
    COMMON("common", ChatFormatting.GRAY, 0),
    UNCOMMON("uncommon", ChatFormatting.GREEN, 1),
    RARE("rare", ChatFormatting.BLUE, 2),
    EPIC("epic", ChatFormatting.DARK_PURPLE, 3),
    LEGENDARY("legendary", ChatFormatting.GOLD, 4);

    public static final Codec<RarityLevel> CODEC = StringRepresentable.fromValues(RarityLevel::values);
    public static final Map<String, RarityLevel> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(RarityLevel::getSerializedName, Function.identity()));
    public static final Map<Integer, RarityLevel> BY_RANK = Arrays.stream(values())
            .collect(Collectors.toMap(RarityLevel::rank, Function.identity()));

    private final String id;
    private final ChatFormatting color;
    private final int rank;

    RarityLevel(String id, ChatFormatting color, int rank) {
        this.id = id;
        this.color = color;
        this.rank = rank;
    }

    /// Gets the color associated with this rarity level for display purposes.
    ///
    /// @return The ChatFormatting color associated with this rarity level.
    public ChatFormatting color() {
        return this.color;
    }

    /// Gets the rank of this rarity level, where higher ranks indicate rarer cards.
    ///
    /// @return The rank of this rarity level.
    public int rank() {
        return this.rank;
    }

    /// Determines if this rarity level is at least as rare as another rarity level.
    ///
    /// @param other The other rarity level to compare against.
    /// @return true if this rarity level is at least as rare as the other, false otherwise.
    public boolean isAtLeast(RarityLevel other) {
        return this.rank >= other.rank;
    }

    /// Returns the next higher rarity level, or an empty Optional if this is already the highest rarity level.
    ///
    /// @return An Optional containing the next higher rarity level, or empty if this is the highest.
    public Optional<RarityLevel> next() {
        return Optional.ofNullable(BY_RANK.get(rank + 1));
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
        return Optional.ofNullable(BY_ID.get(string.toLowerCase(Locale.ROOT)));
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.id;
    }

    @Override
    public String toString() {
        String name = getSerializedName();
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }
}
