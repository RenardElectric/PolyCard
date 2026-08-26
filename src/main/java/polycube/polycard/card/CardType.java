package polycube.polycard.card;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import polycube.polycard.cardEffects.hostile.*;
import polycube.polycard.cardEffects.misc.*;
import polycube.polycard.cardEffects.neutral.*;
import polycube.polycard.cardEffects.passive.*;
import polycube.polycard.utils.Helpers;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static polycube.polycard.utils.Helpers.decimalFormat;
import static polycube.polycard.utils.Helpers.probToStr;

/// Defines each card family: its identifier, acquisition text, group, and supported rarity data.
/// Runtime effects live in the cardEffects classes; this enum references their constants for display text.
public enum CardType implements StringRepresentable {
    // Passive

    COW(CardDefinition.builder("cow", "breeding two cows", CardGroup.PASSIVE, CowEffects::new)
            .addRarity(RarityLevel.UNCOMMON, 0.08f, false, "Regeneration " + (CowEffects.REGEN_EFFECT_AMPLIFIER + 1) + " for " + CowEffects.REGEN_EFFECT_DURATION / 20 + "s when standing still for " + CowEffects.STILL_DELAY / 20 + "s in plains")
            .addRarity(RarityLevel.RARE, 0.025f, false, "Gain resistance " + (CowEffects.RESISTANCE_EFFECT_AMPLIFIER + 1) + " for " + CowEffects.RESISTANCE_EFFECT_DURATION / 20 + "s when near other Cow cards")
            .addRarity(RarityLevel.EPIC, 0.005f, true, "Convert debuffs into Buffs when drinking milk")
            .addRarity(RarityLevel.LEGENDARY, 0.0005f, true, "+" + CowEffects.REGEN_HEALTH_GAIN_HEARTS + " hearts when drinking milk")
            .build()),
    SQUID(CardDefinition.builder("squid", "killing a squid", CardGroup.PASSIVE, SquidEffects::new)
            .addRarity(RarityLevel.RARE, 0.03f, false, probToStr(SquidEffects.BLINDNESS_WHEN_HIT_PROBABILITY) + "% chance to give blindness " + (SquidEffects.BLINDNESS_AMPLIFIER + 1) + " for " + SquidEffects.BLINDNESS_DURATION / 20 + "s when hit")
            .addRarity(RarityLevel.EPIC, 0.006f, true, probToStr(SquidEffects.BLINDNESS_ON_HIT_PROBABILITY) + "% chance to give blindness " + (SquidEffects.BLINDNESS_AMPLIFIER + 1) + " for " + SquidEffects.BLINDNESS_DURATION / 20 + "s on hit")
            .addRarity(RarityLevel.LEGENDARY, 0.0006f, true, "Water breathing " + (SquidEffects.WATER_BREATHING_AMPLIFIER + 1) + " when under water")
            .build()),
    CHICKEN(CardDefinition.builder("chicken", "breeding two chickens", CardGroup.PASSIVE, ChickenEffects::new)
            .addRarity(RarityLevel.COMMON, 0.12f, false, "Lay eggs randomly")
            .addRarity(RarityLevel.UNCOMMON, 0.045f, false, "Thrown eggs hurt")
            .addRarity(RarityLevel.RARE, 0.02f, false, "Speed " + (ChickenEffects.SPEED_EFFECT_AMPLIFIER + 1) + " near other Chickens cards")
            .addRarity(RarityLevel.EPIC, 0.004f, true, probToStr(ChickenEffects.THROW_EGG_PROBABILITY) + "% chance to shoot an egg when hit")
            .addRarity(RarityLevel.LEGENDARY, 0.0004f, true, "Slow Falling when sneaking mid-air")
            .build()),
    BAT(CardDefinition.builder("bat", "killing a bat", CardGroup.PASSIVE, BatEffects::new)
            .addRarity(RarityLevel.RARE, 0.04f, false, "Gain night vision")
            .addRarity(RarityLevel.EPIC, 0.01f, true, "Reveal nearby entity when sneaking")
            .addRarity(RarityLevel.LEGENDARY, 0.0015f, true, "While sneaking, being hit blinds the attacker and grants you Invisibility, Speed, and Invulnerability, but disables your damage. Lasts " + BatEffects.INVISIBILITY_DURATION / 20 + "s and ends early if you stop sneaking. (" + BatEffects.INVISIBILITY_COOLDOWN / 20 + "s cooldown)")
            .build()),
    HORSE(CardDefinition.builder("horse", "taming a horse", CardGroup.PASSIVE, HorseEffects::new)
            .addRarity(RarityLevel.UNCOMMON, 0.08f, false, "Horses you ride take " + probToStr(HorseEffects.DAMAGE_IGNORED_PERCENTAGE) + "% reduced damage")
            .addRarity(RarityLevel.RARE, 0.03f, false, "Jump Boost " + (HorseEffects.JUMP_BOOST_EFFECT_AMPLIFIER + 1) + " when riding a horse")
            .addRarity(RarityLevel.EPIC, 0.008f, true, "Speed " + (HorseEffects.SPEED_EFFECT_AMPLIFIER + 1) + " when riding a horse")
            .addRarity(RarityLevel.LEGENDARY, 0.001f, true, "The more your horse moves, the faster it goes, up to Speed " + (HorseEffects.MAX_SPEED_BOOST + 1))
            .build()),
    GOAT(CardDefinition.builder("goat", "getting rammed by a goat", CardGroup.PASSIVE, GoatEffects::new)
            .addRarity(RarityLevel.UNCOMMON, 0.08f, false, "Take " + probToStr(GoatEffects.KNOCKBACK_REDUCTION) + "% reduced knockback")
            .addRarity(RarityLevel.RARE, 0.03f, false, "Gain Jump Boost " + (GoatEffects.JUMP_BOOST_EFFECT_AMPLIFIER + 1) + " in mountain biomes")
            .addRarity(RarityLevel.EPIC, 0.008f, true, "Sprint hits deal " + decimalFormat(GoatEffects.SPRINT_HIT_EXTRA_KNOCKBACK) + " extra knockback")
            .addRarity(RarityLevel.LEGENDARY, 0.001f, true, "After sprinting for " + GoatEffects.LEGENDARY_SPRINT_DURATION / 20 + "s, sprint hits launch enemies upward (" + decimalFormat(GoatEffects.LEGENDARY_UPWARD_LAUNCH_VELOCITY) + " vertical velocity)")
            .build()),
    TURTLE(CardDefinition.builder("turtle", "breeding two turtles", CardGroup.PASSIVE, TurtleEffects::new)
            .addRarity(RarityLevel.RARE, 0.04f, false, "Gain Resistance " + (TurtleEffects.RESISTANCE_AMPLIFIER + 1) + " while underwater")
            .addRarity(RarityLevel.EPIC, 0.01f, true, "Take reduced damage when sneaking or blocking  (" + probToStr(TurtleEffects.DAMAGE_DECREASE_PROBABILITY) + "% chance per damage point)")
            .addRarity(RarityLevel.LEGENDARY, 0.0015f, true, "Gain Turtle Master 2 for " + TurtleEffects.TURTLE_MASTER_DURATION / 20 + "s when taking damage on low health")
            .build()),

    // Neutral

    IRON_GOLEM(CardDefinition.builder("iron_golem", " summoning an iron golem", CardGroup.NEUTRAL, IronGolemEffects::new)
            .addRarity(RarityLevel.RARE, 0.10f, false, probToStr(IronGolemEffects.RESISTANCE_ON_ATTACKED_PROBABILITY) + "% chance to gain resistance " + (IronGolemEffects.RESISTANCE_AMPLIFIER + 1) + " for " + IronGolemEffects.RESISTANCE_DURATION / 20 + "s when attacked")
            .addRarity(RarityLevel.EPIC, 0.025f, true, "Hitting with fist knock back enemies (" + IronGolemEffects.KNOCKBACK_HIT_COOLDOWN / 20 + "s cooldown)")
            .addRarity(RarityLevel.LEGENDARY, 0.004f, true, "Falling creates shock wave (" + IronGolemEffects.SHOCKWAVE_COOLDOWN / 20 + "s cooldown)")
            .build()),
    ENDERMAN(CardDefinition.builder("enderman", "killing an enderman", CardGroup.NEUTRAL, EnderManEffects::new)
            .addRarity(RarityLevel.UNCOMMON, 0.06f, false, "Resistance " + (EnderManEffects.RESISTANCE_EFFECT_AMPLIFIER + 1) + " in the End")
            .addRarity(RarityLevel.RARE, 0.01f, false, "No ender pearl damage")
            .addRarity(RarityLevel.EPIC, 0.001f, true, "No ender pearl cooldown")
            .addRarity(RarityLevel.LEGENDARY, 0.0001f, true, EnderManEffects.PROJECTILE_DODGE_PROBABILITY + "% chance to dodge projectile")
            .build()),
    PIGLIN(CardDefinition.builder("piglin", "killing a piglin", CardGroup.NEUTRAL, PiglinEffects::new)
            .addRarity(RarityLevel.UNCOMMON, 0.07f, false, "Piglins do not attack you")
            .addRarity(RarityLevel.RARE, 0.03f, false, "Gold food gives a random buff for " + PiglinEffects.BUFF_DURATION / 20 + "s when eaten")
            .addRarity(RarityLevel.EPIC, 0.008f, true, "Piglin brutes do not attack you")
            .addRarity(RarityLevel.LEGENDARY, 0.001f, true, "Gold items are significantly more durable")
            .build()),
    ZOMBIFIED_PIGLIN(CardDefinition.builder("zombified_piglin", "killing a zombified piglin", CardGroup.NEUTRAL, ZombifiedPiglinEffects::new)
            .addRarity(RarityLevel.EPIC, 0.003f, false, "Zombified piglins do not attack you")
            .addRarity(RarityLevel.LEGENDARY, 0.0002f, true, probToStr(ZombifiedPiglinEffects.SPAWN_REINFORCEMENTS_CHANCE) + "% chance to spawn up to " + ZombifiedPiglinEffects.MAX_REINFORCEMENTS + " zombified piglins to help you when hit by a player (" + ZombifiedPiglinEffects.REINFORCEMENT_COOLDOWN / 20 + "s cooldown)")
            .build()),
    BEE(CardDefinition.builder("bee", "collecting honey", CardGroup.NEUTRAL, BeeEffects::new)
            .addRarity(RarityLevel.RARE, 0.07f, false, "Drinking honey gives speed " + (BeeEffects.SPEED_EFFECT_AMPLIFIER + 1) + " for " + BeeEffects.SPEED_EFFECT_DURATION / 20 + "s")
            .addRarity(RarityLevel.EPIC, 0.015f, true, "Drinking honey gives regeneration " + (BeeEffects.REGENERATION_EFFECT_AMPLIFIER + 1) + " for " + BeeEffects.REGENERATION_EFFECT_DURATION / 20 + "s")
            .addRarity(RarityLevel.LEGENDARY, 0.0015f, true, "Drinking honey gives health boost " + (BeeEffects.HEALTH_BOOST_EFFECT_AMPLIFIER + 1) + " for " + BeeEffects.HEALTH_BOOST_EFFECT_DURATION / 20 + "s")
            .build()),
    WOLF(CardDefinition.builder("wolf", "taming a wolf", CardGroup.NEUTRAL, WolfEffects::new)
            .addRarity(RarityLevel.UNCOMMON, 0.08f, false, "Your wolves gain Resistance " + (WolfEffects.RESISTANCE_AMPLIFIER + 1))
            .addRarity(RarityLevel.RARE, 0.035f, false, "Your wolves gain strength " + (WolfEffects.STRENGTH_AMPLIFIER + 1))
            .addRarity(RarityLevel.EPIC, 0.01f, true, "Killing an enemy heals your wolves (" + probToStr(WolfEffects.HEAL_PERCENTAGE) + "% of max health)")
            .addRarity(RarityLevel.LEGENDARY, 0.0015f, true, "When low health, your wolves gain Strength " + (WolfEffects.IMPROVED_STRENGTH_AMPLIFIER + 1) + " and Speed " + (WolfEffects.IMPROVED_SPEED_AMPLIFIER + 1))
            .build()),

    // Hostile

    ENDER_DRAGON(CardDefinition.builder("ender_dragon", "summoning an ender dragon", CardGroup.HOSTILE, EnderDragonEffects::new)
            .addRarity(RarityLevel.RARE, 0.35f, false, "Gliding does not consume durability")
            .addRarity(RarityLevel.EPIC, 0.12f, true, "Cancel all kinetic damage while gliding")
            .addRarity(RarityLevel.LEGENDARY, 0.03f, true, "Any chestplate allows gliding without elytra")
            .build()),
    WITHER(CardDefinition.builder("wither", "summoning a wither", CardGroup.HOSTILE, WitherEffects::new)
            .addRarity(RarityLevel.COMMON, 0.75f, false, probToStr(WitherEffects.WITHER_ROSE_DROP_PROBABILITY) + "% chance that a mob drops a wither rose when killed")
            .addRarity(RarityLevel.UNCOMMON, 0.45f, true, "Immunity to wither effect")
            .addRarity(RarityLevel.RARE, 0.22f, true, probToStr(WitherEffects.WITHER_EFFECT_PROBABILITY) + "% chance to inflict wither " + (WitherEffects.WITHER_EFFECT_AMPLIFIER + 1) + " effect on hit for " + WitherEffects.WITHER_EFFECT_DURATION / 20 + "s")
            .addRarity(RarityLevel.EPIC, 0.09f, true, "Increase damage on enemies with wither effect (" + probToStr(WitherEffects.DAMAGE_INCREASE_PROBABILITY) + "% chance per damage point)")
            .addRarity(RarityLevel.LEGENDARY, 0.035f, true, "Heal based on the damage dealt on enemies with wither effect (" + probToStr(WitherEffects.LIFE_STEAL_PROBABILITY) + "% chance per damage point)")
            .build()),
    ZOMBIE(CardDefinition.builder("zombie", "killing a zombie", CardGroup.HOSTILE, ZombieEffects::new)
            .addRarity(RarityLevel.COMMON, 0.2f, false, "No hunger when eating rotten flesh")
            .addRarity(RarityLevel.UNCOMMON, 0.07f, false, "Rotten flesh gives +" + ZombieEffects.ROTTEN_FLESH_FOOD_INCREASE + " food")
            .addRarity(RarityLevel.RARE, 0.025f, false, "Rotten flesh gives strength " + (ZombieEffects.STRENGTH_EFFECT_AMPLIFIER + 1) + " for " + ZombieEffects.STRENGTH_EFFECT_DURATION / 20 + "s")
            .addRarity(RarityLevel.EPIC, 0.004f, true, "Rotten flesh gives regeneration " + (ZombieEffects.REGENERATION_EFFECT_AMPLIFIER + 1) + " for " + ZombieEffects.REGENERATION_EFFECT_DURATION / 20 + "s")
            .addRarity(RarityLevel.LEGENDARY, 0.00035f, true, "All zombies variants do not attack you")
            .build()),
    CREEPER(CardDefinition.builder("creeper", "killing a creeper", CardGroup.HOSTILE, CreeperEffects::new)
            .addRarity(RarityLevel.UNCOMMON, 0.06f, false, "Explosions damage you " + probToStr(CreeperEffects.EXPLOSION_DAMAGE_REDUCTION) + "% less")
            .addRarity(RarityLevel.RARE, 0.025f, false, "Explosions knock you back " + probToStr(CreeperEffects.EXPLOSION_KNOCKBACK_REDUCTION) + "% less")
            .addRarity(RarityLevel.EPIC, 0.006f, true, "When hit, " + probToStr(CreeperEffects.EXPLOSION_PROBABILITY) + "% chance to create a blast (no blocks damage)")
            .addRarity(RarityLevel.LEGENDARY, 0.001f, true, "Creepers do not attack you")
            .build()),
    ELDER_WEREWOLF(CardDefinition.builder("elder_werewolf", "being bitten by a wild wolf during a full moon at night", CardGroup.HOSTILE, () -> new WerewolfEffects(WerewolfEffects.WerewolfType.ELDER))
            .mutexGroup("werewolf")
            .addRarity(RarityLevel.COMMON, 0.05f, false, WerewolfEffects.WerewolfType.ELDER.description(RarityLevel.COMMON))
            .addRarity(RarityLevel.UNCOMMON, 0.025f, false, WerewolfEffects.WerewolfType.ELDER.description(RarityLevel.UNCOMMON))
            .addRarity(RarityLevel.RARE, 0.01f, false, WerewolfEffects.WerewolfType.ELDER.description(RarityLevel.RARE))
            .addRarity(RarityLevel.EPIC, 0.005f, true, WerewolfEffects.WerewolfType.ELDER.description(RarityLevel.EPIC))
            .addRarity(RarityLevel.LEGENDARY, 0.001f, true, WerewolfEffects.WerewolfType.ELDER.description(RarityLevel.LEGENDARY))
            .build()),
    ALPHA_WEREWOLF(CardDefinition.builder("alpha_werewolf", "being bitten by a wild wolf during a full moon at night", CardGroup.HOSTILE, () -> new WerewolfEffects(WerewolfEffects.WerewolfType.ALPHA))
            .mutexGroup("werewolf")
            .addRarity(RarityLevel.COMMON, 0.05f, false, WerewolfEffects.WerewolfType.ALPHA.description(RarityLevel.COMMON))
            .addRarity(RarityLevel.UNCOMMON, 0.025f, false, WerewolfEffects.WerewolfType.ALPHA.description(RarityLevel.UNCOMMON))
            .addRarity(RarityLevel.RARE, 0.01f, false, WerewolfEffects.WerewolfType.ALPHA.description(RarityLevel.RARE))
            .addRarity(RarityLevel.EPIC, 0.005f, true, WerewolfEffects.WerewolfType.ALPHA.description(RarityLevel.EPIC))
            .addRarity(RarityLevel.LEGENDARY, 0.001f, true, WerewolfEffects.WerewolfType.ALPHA.description(RarityLevel.LEGENDARY))
            .build()),

    // Misc

    INVENTORY(CardDefinition.builder("inventory", "looting a buried treasure", CardGroup.MISC, InventoryEffects::new)
            .addRarity(RarityLevel.LEGENDARY, 0.1f, true, "Keep inventory but on death if no other cards are equipped")
            .build()),
    LIFE(CardDefinition.builder("life", "looting a village", CardGroup.MISC, LifeEffects::new)
            .addRarity(RarityLevel.COMMON, 0.25f, false, "+1 heart but loose one rarity level on death")
            .addRarity(RarityLevel.UNCOMMON, 0.1f, false, "+2 hearts but loose one rarity level on death")
            .addRarity(RarityLevel.RARE, 0.05f, false, "+2 hearts but loose one rarity level on death")
            .addRarity(RarityLevel.EPIC, 0.01f, true, "+2 hearts but loose one rarity level on death")
            .addRarity(RarityLevel.LEGENDARY, 0.005f, true, "+3 hearts but loose one rarity level on death")
            .build()),
    TOTEM(CardDefinition.builder("totem", "looting a mansion", CardGroup.MISC, TotemEffects::new)
            .addRarity(RarityLevel.COMMON, 0.25f, false, "Count as a totem of undying, consumed when used")
            .addRarity(RarityLevel.UNCOMMON, 0.15f, false, "Count as a totem of undying, consumed when used")
            .addRarity(RarityLevel.RARE, 0.10f, false, "Count as a totem of undying, consumed when used")
            .addRarity(RarityLevel.EPIC, 0.05f, true, "Count as a totem of undying, consumed when used")
            .addRarity(RarityLevel.LEGENDARY, 0.01f, true, "Count as a totem of undying, consumed when used")
            .build()),
    LUCKY(CardDefinition.builder("lucky", "looting any structure", CardGroup.MISC, LuckyEffects::new)
            .addRarity(RarityLevel.COMMON, 0.1f, false, "Get a new random card effect every " + decimalFormat(LuckyEffects.TICK_INTERVAL/1200.0) + " minutes (max common rarity)")
            .addRarity(RarityLevel.UNCOMMON, 0.05f, false, "Get a new random card effect every " + decimalFormat(LuckyEffects.TICK_INTERVAL/1200.0) + " minutes (max uncommon rarity)")
            .addRarity(RarityLevel.RARE, 0.01f, false, "Get a new random card effect every " + decimalFormat(LuckyEffects.TICK_INTERVAL/1200.0) + " minutes (max rare rarity)")
            .addRarity(RarityLevel.EPIC, 0.005f, true, "Get a new random card effect every " + decimalFormat(LuckyEffects.TICK_INTERVAL/1200.0) + " minutes (max epic rarity)")
            .addRarity(RarityLevel.LEGENDARY, 0.001f, true, "Get a new random card effect every " + decimalFormat(LuckyEffects.TICK_INTERVAL/1200.0) + " minutes (max legendary rarity)")
            .build()),
    NETHER(CardDefinition.builder("nether", "looting nether structures", CardGroup.MISC, NetherEffects::new)
            .addRarity(RarityLevel.LEGENDARY, 0.15f, true, "Fire resistance but can only set a spawn point in the nether using beds or respawn anchors")
            .build()),
    SPECTATOR(CardDefinition.builder("spectator", "looting a stronghold", CardGroup.MISC, SpectatorEffects::new)
            .addRarity(RarityLevel.LEGENDARY, 0.025f, true, "Invulnerable but cannot hurt anything, die when removing this card")
            .build());

    public static final Codec<CardType> CODEC = StringRepresentable.fromValues(CardType::values);
    public static final Map<String, CardType> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(CardType::getSerializedName, Function.identity()));

    private final CardDefinition definition;
    private static boolean effectsRegistered;

    CardType(CardDefinition definition) {
        this.definition = definition;
    }

    /// Explicitly registers runtime effects after every definition is fully initialized.
    public static void registerEffects() {
        if (effectsRegistered) throw new IllegalStateException("CardType.registerEffects() called more than once");
        for (var cardType : values()) {
            cardType.definition().createEffects().initialize(cardType);
        }
        effectsRegistered = true;
    }

    /// Returns the immutable authoritative definition for this card type.
    public CardDefinition definition() {
        return definition;
    }

    /// Returns the lowest rarity this card type supports.
    public RarityLevel minRarityLevel() {
        return definition().minRarityLevel();
    }

    /// Returns configuration for a supported rarity level.
    public Optional<Rarity> getRarity(RarityLevel rarityLevel) {
        return definition().rarity(rarityLevel);
    }

    /// Returns whether this card type can exist at the given rarity.
    public boolean hasRarity(RarityLevel rarityLevel) {
        return definition().supports(rarityLevel);
    }

    /// Returns all rarity tiers supported by this card type, in enum-rank order.
    public List<Rarity> getRarities() {
        return definition().raritiesList();
    }

    /// Returns the validated rarity-roll distribution for this card type.
    public RarityDistribution getRarityDistribution() {
        return definition().rarityDistribution();
    }

    /// Returns the player-facing acquisition condition.
    public String getCondition() {
        return definition().acquisitionCondition();
    }

    /// Returns the card group this card type belongs to.
    public CardGroup getGroup() {
        return definition().group();
    }

    /// Returns the mutex group this card type belongs to.
    public String getMutexGroup() {
        return definition().mutexGroup();
    }

    /// Returns the card type identifier, formated as "polycard:cardGroup/card_type".
    public Identifier getId() {
        return definition().id();
    }

    /// Parses a serialized card type id.
    public static Optional<CardType> deserialize(String string) {
        return Optional.ofNullable(BY_ID.get(string.toLowerCase(Locale.ROOT)));
    }

    @Override
    public String getSerializedName() {
        return definition().serializedName();
    }

    @Override
    public String toString() {
        return Helpers.identifierToTitleCase(getSerializedName());
    }
}
