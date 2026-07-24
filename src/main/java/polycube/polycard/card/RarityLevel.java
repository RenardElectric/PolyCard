package polycube.polycard.card;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/// Shared rarity ladder used by all card types.
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

    /// Returns the chat color used for this rarity.
    public ChatFormatting color() {
        return this.color;
    }

    /// Returns the ordering rank; higher means rarer.
    public int rank() {
        return this.rank;
    }

    /// Returns whether this rarity includes effects unlocked at the other rarity.
    public boolean isAtLeast(RarityLevel other) {
        return this.rank >= other.rank;
    }

    /// Returns the next rank in the global rarity ladder.
    public Optional<RarityLevel> next() {
        return Optional.ofNullable(BY_RANK.get(rank + 1));
    }

    /// Parses a serialized rarity id.
    public static Optional<RarityLevel> deserialize(String string) {
        return Optional.ofNullable(BY_ID.get(string.toLowerCase(Locale.ROOT)));
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }

    @Override
    public String toString() {
        String name = getSerializedName();
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }
}
