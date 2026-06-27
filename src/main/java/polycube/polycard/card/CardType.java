package polycube.polycard.card;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jspecify.annotations.NonNull;
import polycube.polycard.cardEffects.hostile.WitherEffects;
import polycube.polycard.cardEffects.hostile.ZombieEffects;
import polycube.polycard.cardEffects.neutral.*;
import polycube.polycard.cardEffects.passive.BatEffects;
import polycube.polycard.cardEffects.passive.ChickenEffects;
import polycube.polycard.cardEffects.passive.CowEffects;
import polycube.polycard.cardEffects.passive.SquidEffects;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static polycube.polycard.utils.Helpers.probToStr;

/// Defines each card family: its identifier, acquisition text, group, and supported rarity data.
/// Runtime effects live in the cardEffects classes; this enum references their constants for display text.
public enum CardType implements StringRepresentable {
    // Passive

    COW("cow", "breeding two cows", "passive") {{
        addRarity(RarityLevel.UNCOMMON, 0.08f, false, "Regeneration " + (CowEffects.REGEN_EFFECT_AMPLIFIER + 1) + " for " + CowEffects.REGEN_EFFECT_DURATION / 20 + "s when standing still for " + CowEffects.STILL_DELAY / 20 + "s in plains");
        addRarity(RarityLevel.RARE, 0.025f, false, "Gain resistance " + (CowEffects.RESISTANCE_EFFECT_AMPLIFIER + 1) + " for " + CowEffects.RESISTANCE_EFFECT_DURATION / 20 + "s when near other Cow cards");
        addRarity(RarityLevel.EPIC, 0.005f, true, "Convert debuffs into Buffs when drinking milk");
        addRarity(RarityLevel.LEGENDARY, 0.0005f, true, "+" + CowEffects.REGEN_HEALTH_GAIN_HEARTS + " hearts when drinking milk");
    }},
    SQUID("squid", "killing a squid", "passive") {{
        addRarity(RarityLevel.RARE, 0.03f, false, probToStr(SquidEffects.BLINDNESS_WHEN_HIT_PROBABILITY) + "% chance to give blindness " + (SquidEffects.BLINDNESS_AMPLIFIER + 1) + " for " + SquidEffects.BLINDNESS_DURATION / 20 + "s when hit");
        addRarity(RarityLevel.EPIC, 0.006f, true, probToStr(SquidEffects.BLINDNESS_ON_HIT_PROBABILITY) + "% chance to give blindness " + (SquidEffects.BLINDNESS_AMPLIFIER + 1) + " for " + SquidEffects.BLINDNESS_DURATION / 20 + "s on hit");
        addRarity(RarityLevel.LEGENDARY, 0.0006f, true, "Water breathing " + (SquidEffects.WATER_BREATHING_AMPLIFIER + 1) + " for " + SquidEffects.WATER_BREATHING_DURATION / 20 + "s when under water");
    }},
    CHICKEN("chicken", "breeding two chicken", "passive") {{
        addRarity(RarityLevel.COMMON, 0.12f, false, "Lay eggs randomly");
        addRarity(RarityLevel.UNCOMMON, 0.045f, false, "Thrown eggs hurt");
        addRarity(RarityLevel.RARE, 0.02f, false, "Speed " + (ChickenEffects.SPEED_EFFECT_AMPLIFIER + 1) + " near other Chickens cards");
        addRarity(RarityLevel.EPIC, 0.004f, true, probToStr(ChickenEffects.THROW_EGG_PROBABILITY) + "% chance to shoot an egg when hit");
        addRarity(RarityLevel.LEGENDARY, 0.0004f, true, "Slow Falling when sneaking mid-air");
    }},
    BAT("bat", "killing a Bat", "passive") {{
        addRarity(RarityLevel.RARE, 0.04f, false, "Gain night vision");
        addRarity(RarityLevel.EPIC, 0.01f, true, "Reveal nearby entity when sneaking");
        addRarity(RarityLevel.LEGENDARY, 0.0015f, true, "While sneaking, being hit blinds the attacker and grants you Invisibility, Speed, and Invulnerability, but disables your damage. Lasts " + BatEffects.INVISIBILITY_DURATION + "s and ends early if you stop sneaking.");
    }},

    // Neutral

    IRON_GOLEM("iron_golem", " summoning an Iron Golem", "neutral") {{
        addRarity(RarityLevel.RARE, 0.10f, false, probToStr(IronGolemEffects.RESISTANCE_ON_ATTACKED_PROBABILITY) + "% chance to gain resistance " + (IronGolemEffects.RESISTANCE_AMPLIFIER + 1) + " for " + IronGolemEffects.RESISTANCE_DURATION / 20 + "s when attacked");
        addRarity(RarityLevel.EPIC, 0.025f, true, "Hitting with fist knock back enemies (" + IronGolemEffects.KNOCKBACK_HIT_COOLDOWN / 20 + "s cooldown)");
        addRarity(RarityLevel.LEGENDARY, 0.004f, true, "Falling creates shock wave (" + IronGolemEffects.SHOCKWAVE_COOLDOWN / 20 + "s cooldown)");
    }},
    ENDERMAN("enderman", "killing an Enderman", "neutral") {{
        addRarity(RarityLevel.UNCOMMON, 0.06f, false, "No ender pearl damage");
        addRarity(RarityLevel.RARE, 0.025f, false, "No ender pearl cooldown");
        addRarity(RarityLevel.EPIC, 0.006f, true, EnderManEffects.PROJECTILE_DODGE_PROBABILITY + "% chance to dodge projectile");
        addRarity(RarityLevel.LEGENDARY, 0.001f, true, "Resistance " + (EnderManEffects.RESISTANCE_EFFECT_AMPLIFIER + 1) + " in the End");
    }},
    PIGLIN("piglin", "killing a piglin", "neutral") {{
        addRarity(RarityLevel.UNCOMMON, 0.07f, false, "Piglins do not attack you");
        addRarity(RarityLevel.RARE, 0.03f, false, "Gold food gives a random buff for " + PiglinEffects.BUFF_DURATION / 20 + "s when eaten");
        addRarity(RarityLevel.EPIC, 0.008f, true, "Piglin brutes do not attack you");
        addRarity(RarityLevel.LEGENDARY, 0.001f, true, "Gold items are significantly more durable");
    }},
    ZOMBIFIED_PIGLIN("zombified_piglin", "killing a zombified piglin", "neutral") {{
        addRarity(RarityLevel.EPIC, 0.003f, false, "Zombified piglins do not attack you");
        addRarity(RarityLevel.LEGENDARY, 0.0002f, true, probToStr(ZombifiedPiglinEffects.SPAWN_REINFORCEMENTS_CHANCE) + "% chance to spawn " + ZombifiedPiglinEffects.MAX_REINFORCEMENTS + " zombified piglins to help you when hit by a player");
    }},
    BEE("bee", "collecting honey", "neutral") {{
        addRarity(RarityLevel.RARE, 0.07f, false, "Drinking honey gives speed " + (BeeEffects.SPEED_EFFECT_AMPLIFIER + 1) + " for " + BeeEffects.SPEED_EFFECT_DURATION / 20 + "s");
        addRarity(RarityLevel.EPIC, 0.015f, true, "Drinking honey gives regeneration " + (BeeEffects.REGENERATION_EFFECT_AMPLIFIER + 1) + " for " + BeeEffects.REGENERATION_EFFECT_DURATION / 20 + "s");
        addRarity(RarityLevel.LEGENDARY, 0.0015f, true, "Drinking honey gives health boost " + (BeeEffects.HEALTH_BOOST_EFFECT_AMPLIFIER + 1) + " for " + BeeEffects.HEALTH_BOOST_EFFECT_DURATION / 20 + "s");
    }},

    // Hostile

    ENDER_DRAGON("ender_dragon", "summoning an Ender Dragon", "hostile") {{
        addRarity(RarityLevel.RARE, 0.35f, false, "Gliding does not consume durability");
        addRarity(RarityLevel.EPIC, 0.12f, true, "Cancel all kinetic damage while gliding");
        addRarity(RarityLevel.LEGENDARY, 0.03f, true, "Any chestplate allows gliding without elytra");
    }},
    WITHER("wither", "summoning a Wither", "hostile") {{
        addRarity(RarityLevel.COMMON, 0.75f, false, probToStr(WitherEffects.WITHER_ROSE_DROP_PROBABILITY) + "% chance that a mob drops a wither rose when killed");
        addRarity(RarityLevel.UNCOMMON, 0.45f, true, "Immunity to wither effect");
        addRarity(RarityLevel.RARE, 0.22f, true, probToStr(WitherEffects.WITHER_EFFECT_PROBABILITY) + "% chance to inflict wither " + (WitherEffects.WITHER_EFFECT_AMPLIFIER + 1) + " effect on hit for " + WitherEffects.WITHER_EFFECT_DURATION / 20 + "s");
        addRarity(RarityLevel.EPIC, 0.09f, true, "Increase damage on enemies with wither effect (" + probToStr(WitherEffects.DAMAGE_INCREASE_PROBABILITY) + "% chance per damage point)");
        addRarity(RarityLevel.LEGENDARY, 0.035f, true, "Heal based on the damage dealt on enemies with wither effect (" + probToStr(WitherEffects.LIFE_STEAL_PROBABILITY) + "% chance per damage point)");
    }},
    ZOMBIE("zombie", "killing a zombie", "hostile") {{
        addRarity(RarityLevel.COMMON, 0.20f, false, "No hunger when eating rotten flesh");
        addRarity(RarityLevel.UNCOMMON, 0.07f, false, "Rotten flesh gives +" + ZombieEffects.ROTTEN_FLESH_FOOD_INCREASE + " food");
        addRarity(RarityLevel.RARE, 0.025f, false, "Rotten flesh gives +" + ZombieEffects.ROTTEN_FLESH_FOOD_INCREASE + " food");
        addRarity(RarityLevel.EPIC, 0.004f, true, "Rotten flesh gives strength " + (ZombieEffects.STRENGTH_EFFECT_AMPLIFIER + 1) + " for (" + ZombieEffects.STRENGTH_EFFECT_DURATION / 20 + "s)");
        addRarity(RarityLevel.LEGENDARY, 0.00035f, true, "Rotten flesh gives regeneration " + (ZombieEffects.REGENERATION_EFFECT_AMPLIFIER + 1) + " for (" + ZombieEffects.REGENERATION_EFFECT_DURATION / 20 + "s)");
    }};

    public static final Codec<CardType> CODEC = StringRepresentable.fromValues(CardType::values);
    public static final Map<String, CardType> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(CardType::getSerializedName, Function.identity()));

    private final EnumMap<RarityLevel, Rarity> rarities = new EnumMap<>(RarityLevel.class);
    private final String id;
    private final String condition;
    private final String cardGroup;

    CardType(String id, String condition, String cardGroup) {
        this.id = id;
        this.condition = condition;
        this.cardGroup = cardGroup;
    }

    /// Adds one supported rarity tier to this card type.
    protected Rarity addRarity(RarityLevel rarityLevel, float probability, boolean isEnchanted, String description) {
        var rarity = new Rarity(rarityLevel, probability, isEnchanted, description, HashMultimap.create());
        rarities.put(rarityLevel, rarity);
        return rarity;
    }

    /// Returns the lowest rarity this card type supports.
    public RarityLevel minRarityLevel() {
        return rarities.keySet().iterator().next();
    }

    /// Returns configuration for a supported rarity level.
    public Optional<Rarity> getRarity(RarityLevel rarityLevel) {
        return Optional.ofNullable(rarities.get(rarityLevel));
    }

    /// Returns whether this card type can exist at the given rarity.
    public boolean hasRarity(RarityLevel rarityLevel) {
        return rarities.containsKey(rarityLevel);
    }

    /// Returns all rarity tiers supported by this card type, in enum-rank order.
    public Collection<Rarity> getRarities() {
        return rarities.values();
    }

    /// Returns attribute modifiers from every supported rarity up to and including maxRarity.
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(RarityLevel maxRarity) {
        Multimap<Holder<Attribute>, AttributeModifier> attributes = HashMultimap.create();
        int maxRank = maxRarity.rank();
        for (int i = 0; i <= maxRank; i++) {
            Optional.ofNullable(rarities.get(RarityLevel.BY_RANK.get(i)))
                    .map(Rarity::attributeModifiers)
                    .map(attributes::putAll);
        }
        return attributes;
    }

    /// Returns the player-facing acquisition condition.
    public String getCondition() {
        return condition;
    }

    /// Returns the grouped resource path, formatted as cardGroup/identifier, such as "hostile/zombie".
    public String getFullId() {
        return cardGroup + "/" + id;
    }

    /// Parses a serialized card type id.
    public static Optional<CardType> deserialize(String string) {
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
        var words = getSerializedName().split("_");
        var sj = new StringJoiner(" ");
        for (String word : words) {
            sj.add(word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1));
        }
        return sj.toString();
    }
}
