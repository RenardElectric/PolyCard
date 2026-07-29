package polycube.polycard.card;

import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CardGroup implements StringRepresentable {
    PASSIVE("passive"),
    NEUTRAL("neutral"),
    HOSTILE("hostile");

    public static final Map<String, CardGroup> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(CardGroup::getSerializedName, Function.identity()));

    private final String id;
    CardGroup(String id) {
        this.id = id;
    }

    /// Parses a serialized card group id.
    public static Optional<CardGroup> deserialize(String string) {
        return Optional.ofNullable(BY_ID.get(string.toLowerCase(Locale.ROOT)));
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }
}
