package polycube.polycard.card;

import java.util.*;

public enum Card {
    IRON_GOLEM("iron_golem", "Iron Golem",Rarity.RARE) {{
        addDescription(Rarity.RARE, "20% Chance to gain resistance when hit");
        addDescription(Rarity.EPIC, "Falling creates shock wave (10 sec cooldown)");
        addDescription(Rarity.LEGENDARY, "Hitting with fist knock up enemies (10 sec cooldown)");
    }},
    COW( "cow", "Cow", Rarity.UNCOMMON) {{
        addDescription(Rarity.UNCOMMON, "Regeneration when standing in plains");
        addDescription(Rarity.RARE, "Gain resistance when near other Cow Card");
        addDescription(Rarity.EPIC, "Convert Debuffs into Buffs when drinking milk");
        addDescription(Rarity.LEGENDARY, "+4 Hearts when drinking milk");
    }},
    ENDERMAN("enderman", "Enderman", Rarity.UNCOMMON) {{
        addDescription(Rarity.UNCOMMON, "No ender pearl damage");
        addDescription(Rarity.RARE, "No ender pearl cooldown");
        addDescription(Rarity.EPIC, "20% Chance to dodge projectile");
        addDescription(Rarity.LEGENDARY, "Resistance in the End");
    }};

    private static final Map<String, Card> BY_ID;

    private final String id;
    private final String name;
    private final Rarity minRarity;
    private final Map<Rarity, String> descriptionsByRarity = new HashMap<>();

    static {
        Map<String, Card> byId = new HashMap<>();
        for (Card card : values()) {
            byId.put(card.id, card);
        }
        BY_ID = Collections.unmodifiableMap(byId);
    }

    Card(String id, String name, Rarity minRarity) {
        this.id = id;
        this.name = name;
        this.minRarity = minRarity;
    }

    protected void addDescription(Rarity rarity, String description) {
        descriptionsByRarity.put(rarity, rarity.getColor().toString() + description);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Rarity getMinRarity() {
        return minRarity;
    }

    public static Optional<Card> fromId(String id) {
        return Optional.ofNullable(BY_ID.get(id.toLowerCase()));
    }

    /// Gets all descriptions for this card up to and including the given rarity level.
    ///
    /// @param rarity The rarity level to get descriptions for.
    /// @return A list of descriptions for the given rarity level and all lower rarities.
    public List<String> getDescriptions(Rarity rarity) {
        List<String> descriptions = new ArrayList<>();
        for (Rarity r : Rarity.values()) {
            if (r.ordinal() > rarity.ordinal()) break;
            if (descriptionsByRarity.containsKey(r)) {
                descriptions.add(descriptionsByRarity.get(r));
            }
        }
        return descriptions;
    }
}
