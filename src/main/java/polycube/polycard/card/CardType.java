package polycube.polycard.card;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CardType implements StringRepresentable {
    IRON_GOLEM("iron_golem",Rarity.RARE) {{
        addDescription(Rarity.RARE, "20% Chance to gain resistance when hit");
        addDescription(Rarity.EPIC, "Falling creates shock wave (10 sec cooldown)");
        addDescription(Rarity.LEGENDARY, "Hitting with fist knock up enemies (10 sec cooldown)");
    }},
    COW("cow", Rarity.UNCOMMON) {{
        addDescription(Rarity.UNCOMMON, "Regeneration when standing in plains");
        addDescription(Rarity.RARE, "Gain resistance when near other Cow Card");
        addDescription(Rarity.EPIC, "Convert Debuffs into Buffs when drinking milk");
        addDescription(Rarity.LEGENDARY, "+4 Hearts when drinking milk");
    }},
    ENDERMAN("enderman", Rarity.UNCOMMON) {{
        addDescription(Rarity.UNCOMMON, "No ender pearl damage");
        addDescription(Rarity.RARE, "No ender pearl cooldown");
        addDescription(Rarity.EPIC, "20% Chance to dodge projectile");
        addDescription(Rarity.LEGENDARY, "Resistance in the End");
    }};

    public static final Codec<CardType> CODEC = StringRepresentable.fromValues(CardType::values);
    public static final Map<String, CardType> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(cardType -> cardType.name, Function.identity()));

    private final String name;
    private final Rarity minRarity;
    private final Map<Rarity, Component> descriptionsByRarity = new HashMap<>();

    CardType(String name, Rarity minRarity) {
        this.name = name;
        this.minRarity = minRarity;
    }

    protected void addDescription(Rarity rarity, String description) {
        descriptionsByRarity.put(rarity, Component.literal(description).withStyle(rarity.color()));
    }

    public Rarity minRarity() {
        return minRarity;
    }

    /// Gets all descriptions for this card up to and including the given rarity level.
    ///
    /// @param rarity The rarity level to get descriptions for.
    /// @return A list of descriptions for the given rarity level and all lower rarities.
    public List<Component> getDescriptions(Rarity rarity) {
        List<Component> descriptions = new ArrayList<>();
        for (Rarity r : Rarity.values()) {
            if (r.ordinal() > rarity.ordinal()) break;
            if (descriptionsByRarity.containsKey(r)) {
                descriptions.add(descriptionsByRarity.get(r));
            }
        }
        return descriptions;
    }

    public static Optional<CardType> deserialize(String string) {
        return Optional.ofNullable(BY_NAME.get(string));
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.name;
    }

    @Override
    public String toString() {
        var words = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(word.substring(0, 1).toUpperCase()).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
