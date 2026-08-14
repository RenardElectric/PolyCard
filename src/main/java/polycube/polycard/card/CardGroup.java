package polycube.polycard.card;

import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import polycube.polycard.PolyCard;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CardGroup implements StringRepresentable {
    PASSIVE("passive"),
    NEUTRAL("neutral"),
    HOSTILE("hostile"),
    MISC("misc");

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

    /// Returns the item model identifier path, such as "polycard:cardGroup"
    public Identifier getId() {
        return Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, getSerializedName() + "/" + getSerializedName());
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
