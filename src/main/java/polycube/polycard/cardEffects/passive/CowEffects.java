package polycube.polycard.cardEffects.passive;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.events.callBacks.ItemConsumedEventCallback;
import polycube.polycard.utils.CardRarityConditions;
import polycube.polycard.utils.Helpers;

import java.util.*;

public class CowEffects {
    public static final CardType CARD_TYPE = CardType.COW;

    public static final int REGEN_HEALTH_GAIN_HEARTS = 8;
    public static final int REGEN_HEALTH_GAIN_HEALTH_POINTS = REGEN_HEALTH_GAIN_HEARTS * 2;

    public static final int STILL_DELAY = 20 * 25;
    public static final int REGEN_EFFECT_DURATION = 20 * 5;
    public static final int REGEN_EFFECT_AMPLIFIER = 0;

    public static final int RESISTANCE_EFFECT_DURATION = 20 * 2;
    public static final int RESISTANCE_EFFECT_AMPLIFIER = 0;
    public static final int RESISTANCE_DISTANCE_SQUARED = 25;

    public static final List<Holder<MobEffect>> DEBUFFS = List.of(
            MobEffects.WEAKNESS, MobEffects.MINING_FATIGUE, MobEffects.POISON,
            MobEffects.WITHER, MobEffects.HUNGER, MobEffects.UNLUCK, MobEffects.SLOWNESS, // TODO: No way to get the unluck effect
            MobEffects.BAD_OMEN, MobEffects.INFESTED, MobEffects.OOZING, MobEffects.WEAVING,
            MobEffects.WIND_CHARGED, MobEffects.BLINDNESS, MobEffects.DARKNESS, MobEffects.NAUSEA
    );

    public static final List<Holder<MobEffect>> BUFFS = List.of(
            MobEffects.STRENGTH, MobEffects.HASTE, MobEffects.ABSORPTION,
            MobEffects.REGENERATION, MobEffects.LUCK, MobEffects.SPEED, MobEffects.FIRE_RESISTANCE, // TODO: I do not think that the luck effect even works
            MobEffects.RESISTANCE, MobEffects.WATER_BREATHING, MobEffects.NIGHT_VISION
    );

    private static final Map<UUID, Vec3> lastLocations = new HashMap<>();
    private static final Map<UUID, Integer> lastMoveTimes = new HashMap<>();

    public static void register() {
        ItemConsumedEventCallback.EVENT.register(CowEffects::onBucketUsed);

        Helpers.addPlayerTask((server, player) -> {
            CardRarityConditions.of(player, CARD_TYPE)
                    .hasUncommon(() -> {
                        var pos = player.blockPosition();
                        //noinspection resource
                        var level = player.level();
                        var biome = level.getBiome(pos);
                        if (!biome.is(Biomes.PLAINS)) {
                            lastLocations.remove(player.getUUID());
                            lastMoveTimes.remove(player.getUUID());
                            return;
                        }

                        UUID playerId = player.getUUID();
                        Vec3 currentLocation = player.position();
                        Vec3 previousLocation = lastLocations.put(playerId, currentLocation);
                        if (previousLocation == null || previousLocation.distanceToSqr(currentLocation) > 0.0001D) {
                            lastMoveTimes.remove(playerId);
                            return;
                        }
                        Integer lastMoveTime = lastMoveTimes.put(playerId, lastMoveTimes.getOrDefault(playerId, 0) + 1);
                        if (lastMoveTime != null && lastMoveTime >= STILL_DELAY) {
                            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_EFFECT_DURATION, REGEN_EFFECT_AMPLIFIER, true, true));
                        }
                    }, () -> {
                        lastLocations.remove(player.getUUID());
                        lastMoveTimes.remove(player.getUUID());
                    })
                    .hasRare(() -> {
                        if (Helpers.nearPlayerWithCard(player, CARD_TYPE, RarityLevel.RARE, RESISTANCE_DISTANCE_SQUARED)) {
                            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, RESISTANCE_EFFECT_DURATION, RESISTANCE_EFFECT_AMPLIFIER, true, true));
                        }
                    });
        });
    }

    private static void onBucketUsed(ServerPlayer player, ItemStack itemStack) {
        if (itemStack.getItem() == Items.MILK_BUCKET) {

            CardRarityConditions.of(player, CARD_TYPE)
                    .hasEpic(() -> {
                        var random = new Random();
                        List<MobEffectInstance> effectsGained = new ArrayList<>();
                        for (MobEffectInstance effect : player.getActiveEffects()) {
                            if (DEBUFFS.contains(effect.getEffect())) {
                                Holder<MobEffect> buffType = BUFFS.get(random.nextInt(BUFFS.size()));
                                effectsGained.add(new MobEffectInstance(buffType, effect.getDuration(), effect.getAmplifier())); // TODO: Not sure about the amplifier part, because of bad omen 5...
                            }
                        }

                        Helpers.debug("{} has the epic cow card, giving them buffs for each debuff they had on milk consumption!", player.getName());
                        Helpers.debug("- Debuffs: {}", Arrays.toString(player.getActiveEffects().stream().map(effect -> effect.getEffect().toString()).toArray()));
                        Helpers.debug("- Buffs: {}", Arrays.toString(effectsGained.stream().map(effect -> effect.getEffect().toString()).toArray()));
                        Helpers.runLater(1, _ -> {
                            for (MobEffectInstance effect : effectsGained) {
                                player.addEffect(effect);
                            }
                        });
                    })
                    .hasLegendary(() -> {
                        Helpers.debug("{} has the legendary cow card, giving them extra health on milk consumption!", player.getName());
                        player.heal(REGEN_HEALTH_GAIN_HEALTH_POINTS);
                    });
        }
    }
}
