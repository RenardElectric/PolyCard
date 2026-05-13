package polycube.polycard.card;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CardType implements StringRepresentable {
    IRON_GOLEM("iron_golem") {{
        addRarity(RarityLevel.RARE, 15, false, "20% Chance to gain resistance when hit");
        addRarity(RarityLevel.EPIC, 9, true, "Falling creates shock wave (10 sec cooldown)");
        addRarity(RarityLevel.LEGENDARY, 1, true, "Hitting with fist knock up enemies (10 sec cooldown)");
    }},
    COW("cow") {{
        addRarity(RarityLevel.UNCOMMON, 25, false, "Regeneration when standing in plains");
        addRarity(RarityLevel.RARE, 15, false, "Gain resistance when near other Cow Card");
        addRarity(RarityLevel.EPIC, 9, true, "Convert Debuffs into Buffs when drinking milk");
        addRarity(RarityLevel.LEGENDARY, 1, true, "+4 Hearts when drinking milk");
    }},
    ENDERMAN("enderman") {{
        addRarity(RarityLevel.UNCOMMON, 25, false, "No ender pearl damage");
        addRarity(RarityLevel.RARE, 15, false, "No ender pearl cooldown");
        addRarity(RarityLevel.EPIC, 9, true, "20% Chance to dodge projectile");
        addRarity(RarityLevel.LEGENDARY, 1, true, "Resistance in the End");
    }};

    public static final Codec<CardType> CODEC = StringRepresentable.fromValues(CardType::values);
    public static final Map<String, CardType> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(cardType -> cardType.name, Function.identity()));

    private final String name;
    private final Map<RarityLevel, Rarity> rarities = new HashMap<>();

    CardType(String name) {
        this.name = name;
    }

    protected void addRarity(RarityLevel rarityLevel, int probability, boolean isEnchanted, String description) {
        rarities.put(rarityLevel, new Rarity(rarityLevel, probability, isEnchanted, description));
    }

    /// Gets the minimum rarity level available for this card.
    ///
    /// @return The minimum rarity level available for this card.
    public RarityLevel minRarityLevel() {
        return rarities.keySet().stream().min(Comparator.comparingInt(Enum::ordinal)).orElseThrow();
    }

    /// Gets a list of all rarities available for this card, sorted by their rarity level.
    ///
    /// @return A list of rarities for this card, sorted by rarity level.
    public List<Rarity> getRarities() {
        return rarities.values().stream()
                .sorted(Comparator.comparing(r -> r.rarityLevel().ordinal()))
                .toList();
    }

    /// Gets all descriptions for this card up to and including the given rarity level.
    ///
    /// @param maxRarity The rarity level to get descriptions for.
    /// @return A list of descriptions for the given rarity level and all lower rarities.
    public List<Component> getDescriptions(RarityLevel maxRarity) {
        var descriptions = new ArrayList<Component>();
        for (var rarity : rarities.keySet().stream().sorted(Comparator.comparing(Enum::ordinal)).toList()) {
            descriptions.add(rarities.get(rarity).getFormattedDescription());
            if (rarity == maxRarity) break;
        }
        return descriptions;
    }

    /// Gets the probability of obtaining this card at a given rarity level.
    ///
    /// @param rarityLevel The rarity level to get the probability for.
    /// @return The probability of obtaining this card at the given rarity level, or 0 if the card does not have that rarity level.
    public int getProbability(RarityLevel rarityLevel) {
        if (rarities.containsKey(rarityLevel)) {
            return rarities.get(rarityLevel).probability();
        }
        return 0;
    }

    /// Checks if the card has an enchanted glint for a given rarity level.
    ///
    /// @param rarityLevel The rarity level to check for enchantment.
    /// @return true if the card has an enchanted glint for the given rarity level, false otherwise.
    public boolean isEnchanted(RarityLevel rarityLevel) {
        if (rarities.containsKey(rarityLevel)) {
            return rarities.get(rarityLevel).isEnchanted();
        }
        return false;
    }

    /// Deserializes a CardType from a string.
    ///
    /// @param string the string to deserialize
    /// @return an Optional containing the deserialized CardType, or empty if the string is invalid
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
