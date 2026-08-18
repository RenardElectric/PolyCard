package polycube.polycard.card;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;
import polycube.polycard.card.CardDefinition.CardDefinitionBuilder;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.cardEffects.hostile.*;
import polycube.polycard.cardEffects.misc.*;
import polycube.polycard.cardEffects.neutral.*;
import polycube.polycard.cardEffects.passive.*;
import polycube.polycard.utils.Helpers;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static polycube.polycard.utils.Helpers.decimalFormat;
import static polycube.polycard.utils.Helpers.probToStr;

/// Defines each card family: its identifier, acquisition text, group, and supported rarity data.
/// Runtime effects live in the cardEffects classes; this enum references their constants for display text.
public enum CardType implements StringRepresentable {
    // Passive

    COW("cow", "breeding two cows", CardGroup.PASSIVE, CowEffects::new) {{
        addRarity(RarityLevel.UNCOMMON, 0.08f, false, "Regeneration " + (CowEffects.REGEN_EFFECT_AMPLIFIER + 1) + " for " + CowEffects.REGEN_EFFECT_DURATION / 20 + "s when standing still for " + CowEffects.STILL_DELAY / 20 + "s in plains");
        addRarity(RarityLevel.RARE, 0.025f, false, "Gain resistance " + (CowEffects.RESISTANCE_EFFECT_AMPLIFIER + 1) + " for " + CowEffects.RESISTANCE_EFFECT_DURATION / 20 + "s when near other Cow cards");
        addRarity(RarityLevel.EPIC, 0.005f, true, "Convert debuffs into Buffs when drinking milk");
        addRarity(RarityLevel.LEGENDARY, 0.0005f, true, "+" + CowEffects.REGEN_HEALTH_GAIN_HEARTS + " hearts when drinking milk");
    }},
    SQUID("squid", "killing a squid", CardGroup.PASSIVE, SquidEffects::new) {{
        addRarity(RarityLevel.RARE, 0.03f, false, probToStr(SquidEffects.BLINDNESS_WHEN_HIT_PROBABILITY) + "% chance to give blindness " + (SquidEffects.BLINDNESS_AMPLIFIER + 1) + " for " + SquidEffects.BLINDNESS_DURATION / 20 + "s when hit");
        addRarity(RarityLevel.EPIC, 0.006f, true, probToStr(SquidEffects.BLINDNESS_ON_HIT_PROBABILITY) + "% chance to give blindness " + (SquidEffects.BLINDNESS_AMPLIFIER + 1) + " for " + SquidEffects.BLINDNESS_DURATION / 20 + "s on hit");
        addRarity(RarityLevel.LEGENDARY, 0.0006f, true, "Water breathing " + (SquidEffects.WATER_BREATHING_AMPLIFIER + 1) + " when under water");
    }},
    CHICKEN("chicken", "breeding two chickens", CardGroup.PASSIVE, ChickenEffects::new) {{
        addRarity(RarityLevel.COMMON, 0.12f, false, "Lay eggs randomly");
        addRarity(RarityLevel.UNCOMMON, 0.045f, false, "Thrown eggs hurt");
        addRarity(RarityLevel.RARE, 0.02f, false, "Speed " + (ChickenEffects.SPEED_EFFECT_AMPLIFIER + 1) + " near other Chickens cards");
        addRarity(RarityLevel.EPIC, 0.004f, true, probToStr(ChickenEffects.THROW_EGG_PROBABILITY) + "% chance to shoot an egg when hit");
        addRarity(RarityLevel.LEGENDARY, 0.0004f, true, "Slow Falling when sneaking mid-air");
    }},
    BAT("bat", "killing a bat", CardGroup.PASSIVE, BatEffects::new) {{
        addRarity(RarityLevel.RARE, 0.04f, false, "Gain night vision");
        addRarity(RarityLevel.EPIC, 0.01f, true, "Reveal nearby entity when sneaking");
        addRarity(RarityLevel.LEGENDARY, 0.0015f, true, "While sneaking, being hit blinds the attacker and grants you Invisibility, Speed, and Invulnerability, but disables your damage. Lasts " + BatEffects.INVISIBILITY_DURATION / 20 + "s and ends early if you stop sneaking. (" + BatEffects.INVISIBILITY_COOLDOWN / 20 + "s cooldown)");
    }},
    HORSE("horse", "taming a horse", CardGroup.PASSIVE, HorseEffects::new) {{
        addRarity(RarityLevel.UNCOMMON, 0.08f, false, "Horses you ride take " + probToStr(HorseEffects.DAMAGE_IGNORED_PERCENTAGE) + "% reduced damage");
        addRarity(RarityLevel.RARE, 0.03f, false, "Jump Boost " + (HorseEffects.JUMP_BOOST_EFFECT_AMPLIFIER + 1) + " when riding a horse");
        addRarity(RarityLevel.EPIC, 0.008f, true, "Speed " + (HorseEffects.SPEED_EFFECT_AMPLIFIER + 1) + " when riding a horse");
        addRarity(RarityLevel.LEGENDARY, 0.001f, true, "The more your horse moves, the faster it goes, up to Speed " + (HorseEffects.MAX_SPEED_BOOST + 1));
    }},
    TURTLE("turtle", "breeding two turtles", CardGroup.PASSIVE, TurtleEffects::new) {{
        addRarity(RarityLevel.RARE, 0.04f, false, "Gain Resistance " + (TurtleEffects.RESISTANCE_AMPLIFIER + 1) + " while underwater");
        addRarity(RarityLevel.EPIC, 0.01f, true, "Take reduced damage when sneaking or blocking  (" + probToStr(TurtleEffects.DAMAGE_DECREASE_PROBABILITY) + "% chance per damage point)");
        addRarity(RarityLevel.LEGENDARY, 0.0015f, true, "Gain Turtle Master 2 for " + TurtleEffects.TURTLE_MASTER_DURATION / 20 + "s when taking damage on low health");
    }},

    // Neutral

    IRON_GOLEM("iron_golem", " summoning an iron golem", CardGroup.NEUTRAL, IronGolemEffects::new) {{
        addRarity(RarityLevel.RARE, 0.10f, false, probToStr(IronGolemEffects.RESISTANCE_ON_ATTACKED_PROBABILITY) + "% chance to gain resistance " + (IronGolemEffects.RESISTANCE_AMPLIFIER + 1) + " for " + IronGolemEffects.RESISTANCE_DURATION / 20 + "s when attacked");
        addRarity(RarityLevel.EPIC, 0.025f, true, "Hitting with fist knock back enemies (" + IronGolemEffects.KNOCKBACK_HIT_COOLDOWN / 20 + "s cooldown)");
        addRarity(RarityLevel.LEGENDARY, 0.004f, true, "Falling creates shock wave (" + IronGolemEffects.SHOCKWAVE_COOLDOWN / 20 + "s cooldown)");
    }},
    ENDERMAN("enderman", "killing an enderman", CardGroup.NEUTRAL, EnderManEffects::new) {{
        addRarity(RarityLevel.UNCOMMON, 0.06f, false, "Resistance " + (EnderManEffects.RESISTANCE_EFFECT_AMPLIFIER + 1) + " in the End");
        addRarity(RarityLevel.RARE, 0.01f, false, "No ender pearl damage");
        addRarity(RarityLevel.EPIC, 0.001f, true, "No ender pearl cooldown");
        addRarity(RarityLevel.LEGENDARY, 0.0001f, true, EnderManEffects.PROJECTILE_DODGE_PROBABILITY + "% chance to dodge projectile");
    }},
    PIGLIN("piglin", "killing a piglin", CardGroup.NEUTRAL, PiglinEffects::new) {{
        addRarity(RarityLevel.UNCOMMON, 0.07f, false, "Piglins do not attack you");
        addRarity(RarityLevel.RARE, 0.03f, false, "Gold food gives a random buff for " + PiglinEffects.BUFF_DURATION / 20 + "s when eaten");
        addRarity(RarityLevel.EPIC, 0.008f, true, "Piglin brutes do not attack you");
        addRarity(RarityLevel.LEGENDARY, 0.001f, true, "Gold items are significantly more durable");
    }},
    ZOMBIFIED_PIGLIN("zombified_piglin", "killing a zombified piglin", CardGroup.NEUTRAL, ZombifiedPiglinEffects::new) {{
        addRarity(RarityLevel.EPIC, 0.003f, false, "Zombified piglins do not attack you");
        addRarity(RarityLevel.LEGENDARY, 0.0002f, true, probToStr(ZombifiedPiglinEffects.SPAWN_REINFORCEMENTS_CHANCE) + "% chance to spawn up to " + ZombifiedPiglinEffects.MAX_REINFORCEMENTS + " zombified piglins to help you when hit by a player (" + ZombifiedPiglinEffects.REINFORCEMENT_COOLDOWN / 20 + "s cooldown)");
    }},
    BEE("bee", "collecting honey", CardGroup.NEUTRAL, BeeEffects::new) {{
        addRarity(RarityLevel.RARE, 0.07f, false, "Drinking honey gives speed " + (BeeEffects.SPEED_EFFECT_AMPLIFIER + 1) + " for " + BeeEffects.SPEED_EFFECT_DURATION / 20 + "s");
        addRarity(RarityLevel.EPIC, 0.015f, true, "Drinking honey gives regeneration " + (BeeEffects.REGENERATION_EFFECT_AMPLIFIER + 1) + " for " + BeeEffects.REGENERATION_EFFECT_DURATION / 20 + "s");
        addRarity(RarityLevel.LEGENDARY, 0.0015f, true, "Drinking honey gives health boost " + (BeeEffects.HEALTH_BOOST_EFFECT_AMPLIFIER + 1) + " for " + BeeEffects.HEALTH_BOOST_EFFECT_DURATION / 20 + "s");
    }},
    WOLF("wolf", "taming a wolf", CardGroup.NEUTRAL, WolfEffects::new) {{
        addRarity(RarityLevel.UNCOMMON, 0.08f, false, "Your wolves gain Resistance " + (WolfEffects.RESISTANCE_AMPLIFIER + 1));
        addRarity(RarityLevel.RARE, 0.035f, false, "Your wolves gain strength " + (WolfEffects.STRENGTH_AMPLIFIER + 1));
        addRarity(RarityLevel.EPIC, 0.01f, true, "Killing an enemy heals your wolves (" + probToStr(WolfEffects.HEAL_PERCENTAGE) + "% of max health)");
        addRarity(RarityLevel.LEGENDARY, 0.0015f, true, "When low health, your wolves gain Strength " + (WolfEffects.IMPROVED_STRENGTH_AMPLIFIER + 1) + " and Speed " + (WolfEffects.IMPROVED_SPEED_AMPLIFIER + 1));
    }},

    // Hostile

    ENDER_DRAGON("ender_dragon", "summoning an ender dragon", CardGroup.HOSTILE, EnderDragonEffects::new) {{
        addRarity(RarityLevel.RARE, 0.35f, false, "Gliding does not consume durability");
        addRarity(RarityLevel.EPIC, 0.12f, true, "Cancel all kinetic damage while gliding");
        addRarity(RarityLevel.LEGENDARY, 0.03f, true, "Any chestplate allows gliding without elytra");
    }},
    WITHER("wither", "summoning a wither", CardGroup.HOSTILE, WitherEffects::new) {{
        addRarity(RarityLevel.COMMON, 0.75f, false, probToStr(WitherEffects.WITHER_ROSE_DROP_PROBABILITY) + "% chance that a mob drops a wither rose when killed");
        addRarity(RarityLevel.UNCOMMON, 0.45f, true, "Immunity to wither effect");
        addRarity(RarityLevel.RARE, 0.22f, true, probToStr(WitherEffects.WITHER_EFFECT_PROBABILITY) + "% chance to inflict wither " + (WitherEffects.WITHER_EFFECT_AMPLIFIER + 1) + " effect on hit for " + WitherEffects.WITHER_EFFECT_DURATION / 20 + "s");
        addRarity(RarityLevel.EPIC, 0.09f, true, "Increase damage on enemies with wither effect (" + probToStr(WitherEffects.DAMAGE_INCREASE_PROBABILITY) + "% chance per damage point)");
        addRarity(RarityLevel.LEGENDARY, 0.035f, true, "Heal based on the damage dealt on enemies with wither effect (" + probToStr(WitherEffects.LIFE_STEAL_PROBABILITY) + "% chance per damage point)");
    }},
    ZOMBIE("zombie", "killing a zombie", CardGroup.HOSTILE, ZombieEffects::new) {{
        addRarity(RarityLevel.COMMON, 0.2f, false, "No hunger when eating rotten flesh");
        addRarity(RarityLevel.UNCOMMON, 0.07f, false, "Rotten flesh gives +" + ZombieEffects.ROTTEN_FLESH_FOOD_INCREASE + " food");
        addRarity(RarityLevel.RARE, 0.025f, false, "Rotten flesh gives strength " + (ZombieEffects.STRENGTH_EFFECT_AMPLIFIER + 1) + " for " + ZombieEffects.STRENGTH_EFFECT_DURATION / 20 + "s");
        addRarity(RarityLevel.EPIC, 0.004f, true, "Rotten flesh gives regeneration " + (ZombieEffects.REGENERATION_EFFECT_AMPLIFIER + 1) + " for " + ZombieEffects.REGENERATION_EFFECT_DURATION / 20 + "s");
        addRarity(RarityLevel.LEGENDARY, 0.00035f, true, "All zombies variants do not attack you");
    }},
    CREEPER("creeper", "killing a creeper", CardGroup.HOSTILE, CreeperEffects::new) {{
        addRarity(RarityLevel.UNCOMMON, 0.06f, false, "Explosions damage you " + probToStr(CreeperEffects.EXPLOSION_DAMAGE_REDUCTION) + "% less");
        addRarity(RarityLevel.RARE, 0.025f, false, "Explosions knock you back " + probToStr(CreeperEffects.EXPLOSION_KNOCKBACK_REDUCTION) + "% less");
        addRarity(RarityLevel.EPIC, 0.006f, true, "When hit, " + probToStr(CreeperEffects.EXPLOSION_PROBABILITY) + "% chance to create a blast (no blocks damage)");
        addRarity(RarityLevel.LEGENDARY, 0.001f, true, "Creepers do not attack you");
    }},
    ELDER_WEREWOLF("elder_werewolf", "being bitten by a wild wolf during a full moon at night", CardGroup.HOSTILE, "werewolf", () -> new WerewolfEffects(WerewolfEffects.WerewolfType.ELDER)) {{
        addRarity(RarityLevel.COMMON, 0.05f, false, "±0.15 health depending on the moon phase");
        addRarity(RarityLevel.UNCOMMON, 0.025f, false, "±0.15 health depending on the moon phase");
        addRarity(RarityLevel.RARE, 0.01f, false, "±0.15 health depending on the moon phase");
        addRarity(RarityLevel.EPIC, 0.005f, true, "±0.15 health depending on the moon phase");
        addRarity(RarityLevel.LEGENDARY, 0.001f, true, "±0.15 health depending on the moon phase");
    }},
    ALPHA_WEREWOLF("alpha_werewolf", "being bitten by a wild wolf during a full moon at night", CardGroup.HOSTILE, "werewolf", () -> new WerewolfEffects(WerewolfEffects.WerewolfType.ALPHA)) {{
        addRarity(RarityLevel.COMMON, 0.05f, false, "±0.15 damage depending on the moon phase");
        addRarity(RarityLevel.UNCOMMON, 0.025f, false, "±0.15 damage depending on the moon phase");
        addRarity(RarityLevel.RARE, 0.01f, false, "±0.15damage depending on the moon phase");
        addRarity(RarityLevel.EPIC, 0.005f, true, "±0.15 damage depending on the moon phase");
        addRarity(RarityLevel.LEGENDARY, 0.001f, true, "±0.15 damage depending on the moon phase");
    }},

    // Misc

    INVENTORY("inventory", "TODO", CardGroup.MISC, InventoryEffects::new) {{
        addRarity(RarityLevel.LEGENDARY, 1.0f, true, "Keep inventory but on death loose one rarity level in a random equipped card");
    }},
    LIFE("life", "TODO", CardGroup.MISC, LifeEffects::new) {{
        addRarity(RarityLevel.COMMON, 1.0f, false, "+1 heart but loose one rarity level on death");
        addRarity(RarityLevel.UNCOMMON, 1.0f, false, "+2 hearts but loose one rarity level on death");
        addRarity(RarityLevel.RARE, 1.0f, false, "+2 hearts but loose one rarity level on death");
        addRarity(RarityLevel.EPIC, 1.0f, true, "+2 hearts but loose one rarity level on death");
        addRarity(RarityLevel.LEGENDARY, 1.0f, true, "+3 hearts but loose one rarity level on death");
    }},
    TOTEM("totem", "looting a mansion", CardGroup.MISC, TotemEffects::new) {{
        addRarity(RarityLevel.COMMON, 0.25f, false, "Count as a totem of undying, consumed when used");
        addRarity(RarityLevel.UNCOMMON, 0.15f, false, "Count as a totem of undying, consumed when used");
        addRarity(RarityLevel.RARE, 0.10f, false, "Count as a totem of undying, consumed when used");
        addRarity(RarityLevel.EPIC, 0.05f, true, "Count as a totem of undying, consumed when used");
        addRarity(RarityLevel.LEGENDARY, 0.01f, true, "Count as a totem of undying, consumed when used");
    }},
    LUCKY("lucky", "looting any structure", CardGroup.MISC, LuckyEffects::new) {{
        addRarity(RarityLevel.COMMON, 0.1f, false, "Get a new random card effect every " + decimalFormat(LuckyEffects.TICK_INTERVAL/1200.0) + " minutes (max common rarity)");
        addRarity(RarityLevel.UNCOMMON, 0.05f, false, "Get a new random card effect every " + decimalFormat(LuckyEffects.TICK_INTERVAL/1200.0) + " minutes (max uncommon rarity)");
        addRarity(RarityLevel.RARE, 0.01f, false, "Get a new random card effect every " + decimalFormat(LuckyEffects.TICK_INTERVAL/1200.0) + " minutes (max rare rarity)");
        addRarity(RarityLevel.EPIC, 0.005f, true, "Get a new random card effect every " + decimalFormat(LuckyEffects.TICK_INTERVAL/1200.0) + " minutes (max epic rarity)");
        addRarity(RarityLevel.LEGENDARY, 0.001f, true, "Get a new random card effect every " + decimalFormat(LuckyEffects.TICK_INTERVAL/1200.0) + " minutes (max legendary rarity)");
    }},
    NETHER("nether", "looting nether structures", CardGroup.MISC, NetherEffects::new) {{
        addRarity(RarityLevel.LEGENDARY, 0.15f, true, "Fire resistance but can only respawn in the nether");
    }},
    SPECTATOR("spectator", "TODO", CardGroup.MISC, SpectatorEffects::new) {{
        addRarity(RarityLevel.LEGENDARY, 1.0f, true, "Invulnerable but cannot hurt anything, die when removing this card");
    }};

    static {
        for (var cardType : values()) {
            cardType.finalizeDefinition();
        }
    }

    public static final Codec<CardType> CODEC = StringRepresentable.fromValues(CardType::values);
    public static final Map<String, CardType> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(CardType::getSerializedName, Function.identity()));

    private @Nullable CardDefinitionBuilder definitionBuilder;
    private @Nullable CardDefinition definition;
    private static boolean effectsRegistered;

    CardType(String id, String condition, CardGroup cardGroup, Supplier<CardEffects> effectRegistration) {
        this(id, condition, cardGroup, "", effectRegistration);
    }

    CardType(String id, String condition, CardGroup cardGroup, String mutexGroup, Supplier<CardEffects> effectRegistration) {
        definitionBuilder = CardDefinition.builder(id, condition, cardGroup, effectRegistration)
                .mutexGroup(mutexGroup);
    }

    /// Adds one supported rarity tier to this card type.
    protected void addRarity(RarityLevel rarityLevel, float probability, boolean isEnchanted, String description) {
        Objects.requireNonNull(definitionBuilder, "Cannot add rarities after a card definition is finalized")
                .addRarity(rarityLevel, probability, isEnchanted, description);
    }

    /// Builds the immutable definition after an enum constant's rarity initializer has run.
    private void finalizeDefinition() {
        var builder = Objects.requireNonNull(definitionBuilder, "Card definition is already finalized");
        definition = builder.build();
        definitionBuilder = null;
    }

    /// Explicitly registers runtime effects after every definition is fully initialized.
    public static void registerEffects() {
        if (effectsRegistered) {
            return;
        }
        effectsRegistered = true;
        for (var cardType : values()) {
            cardType.definition().createEffects().initialize(cardType);
        }
    }

    /// Returns the immutable authoritative definition for this card type.
    public CardDefinition definition() {
        return Objects.requireNonNull(definition, "Card definition is unavailable during enum construction");
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
        return definition().rarities();
    }

    /// Returns the validated cumulative probability distribution for this card type.
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
