package polycube.polycard.cardEffects.passive;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;
import polycube.polycard.PolyCard;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.ItemConsumedEventCallback;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;
import polycube.polycard.utils.EffectHelpers;
import polycube.polycard.utils.Helpers;

import java.util.ArrayList;
import java.util.List;

public class CowEffects extends CardEffects implements PlayerTickEventCallback, ItemConsumedEventCallback {
    public static final int REGEN_HEALTH_GAIN_HEARTS = 8;

    public static final int STILL_DELAY = 20 * 20;
    public static final int REGEN_EFFECT_DURATION = 20 * 5;
    public static final int REGEN_EFFECT_AMPLIFIER = 0;

    public static final int RESISTANCE_EFFECT_DURATION = 20 * 2;
    public static final int RESISTANCE_EFFECT_AMPLIFIER = 0;
    public static final int RESISTANCE_DISTANCE_SQUARED = 25;
    public static final int MAX_CONVERTED_EFFECT_DURATION = 20 * 60;
    public static final int MAX_CONVERTED_EFFECT_AMPLIFIER = 2;

    public static final List<Holder<MobEffect>> BUFFS = List.of(
            MobEffects.STRENGTH, MobEffects.HASTE, MobEffects.ABSORPTION,
            MobEffects.REGENERATION, MobEffects.LUCK, MobEffects.SPEED, MobEffects.FIRE_RESISTANCE,
            MobEffects.RESISTANCE, MobEffects.WATER_BREATHING, MobEffects.NIGHT_VISION
    );

    private final PlayerState<Vec3> lastLocations = new PlayerState<>();
    private final PlayerState<Integer> lastMoveTimes = new PlayerState<>();

    @Override
    public void onPlayerTick(MinecraftServer server, ServerPlayer player) {
        var rarity = equippedRarityLevel(player);
        if (rarity == null || !rarity.isAtLeast(RarityLevel.UNCOMMON)) {
            clearMovementState(player);
            return;
        }

        var pos = player.blockPosition();
        var level = player.level();
        if (!level.getBiome(pos).is(Biomes.PLAINS)) {
            clearMovementState(player);
        } else {
            Vec3 currentLocation = player.position();
            Vec3 previousLocation = lastLocations.put(player, currentLocation);
            if (previousLocation == null || previousLocation.distanceToSqr(currentLocation) > 0.0001D) {
                lastMoveTimes.remove(player);
            } else {
                int stillTicks = lastMoveTimes.getOrDefault(player, 0) + 1;
                lastMoveTimes.put(player, stillTicks);
                if (stillTicks >= STILL_DELAY) {
                    EffectHelpers.refreshPersistentEffect(player, MobEffects.REGENERATION, REGEN_EFFECT_AMPLIFIER);
                }
            }
        }

        if (rarity.isAtLeast(RarityLevel.RARE) && nearPlayerWithCard(player, RarityLevel.RARE, RESISTANCE_DISTANCE_SQUARED)) {
            EffectHelpers.refreshPersistentEffect(player, MobEffects.RESISTANCE, RESISTANCE_EFFECT_AMPLIFIER);
        }
    }

    private void clearMovementState(ServerPlayer player) {
        lastLocations.remove(player);
        lastMoveTimes.remove(player);
    }

    @Override
    public void onItemConsumed(ServerPlayer player, ItemStack itemStack) {
        if (itemStack.getItem().equals(Items.MILK_BUCKET)) {

            conditionsFor(player)
                    .hasEpic(() -> {
                        List<MobEffectInstance> effectsGained = new ArrayList<>();
                        var availableBuffs = new ArrayList<>(BUFFS);
                        for (MobEffectInstance effect : player.getActiveEffects()) {
                            if (effect.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL) {
                                if (availableBuffs.isEmpty()) {
                                    availableBuffs.addAll(BUFFS);
                                }
                                Holder<MobEffect> buffType = availableBuffs.remove(player.getRandom().nextInt(availableBuffs.size()));
                                int duration = effect.isInfiniteDuration()
                                        ? MAX_CONVERTED_EFFECT_DURATION
                                        : Math.min(effect.getDuration(), MAX_CONVERTED_EFFECT_DURATION);
                                int amplifier = Math.min(effect.getAmplifier(), MAX_CONVERTED_EFFECT_AMPLIFIER);
                                effectsGained.add(new MobEffectInstance(buffType, duration, amplifier));
                            }
                        }

                        Helpers.debug("{} converted {} non-beneficial effect(s) with an Epic Cow card", player.getName().getString(), effectsGained.size());
                        PolyCard.scheduler().runLater(0, _ -> {
                            for (MobEffectInstance effect : effectsGained) {
                                player.addEffect(effect);
                            }
                        });
                    })
                    .hasLegendary(() -> {
                        Helpers.debug("{} has the legendary cow card, giving them extra health on milk consumption!", player.getName());
                        player.heal(REGEN_HEALTH_GAIN_HEARTS * 2);
                    });
        }
    }
}
