package polycube.polycard.cardEffects.neutral;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.jspecify.annotations.NonNull;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.PlayerKillEventCallback;
import polycube.polycard.utils.CardRarityConditions;
import polycube.polycard.utils.Helpers;

import java.util.*;

public class WolfEffects extends CardEffects implements PlayerKillEventCallback, ServerTickEvents.EndLevelTick {

    public static final int RESISTANCE_AMPLIFIER = 0;
    public static final int STRENGTH_AMPLIFIER = 0;
    public static final int LOW_HEALTH_THRESHOLD = 8;
    public static final int IMPROVED_STRENGTH_AMPLIFIER = 1;
    public static final int IMPROVED_SPEED_AMPLIFIER = 1;
    public static final float HEAL_PERCENTAGE = 0.25f;

    private static final Map<UUID, Set<Wolf>> playerKill = new HashMap<>();

    @Override
    public void onEndTick(@NonNull ServerLevel level) {
        for (var wolf : level.getEntities(EntityTypes.WOLF, LivingEntity::isAlive)) {
            if (wolf.getRootOwner() instanceof ServerPlayer player) {
                CardRarityConditions.of(player, cardType)
                        .hasUncommon(() -> wolf.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 2, RESISTANCE_AMPLIFIER, true, false)))
                        .hasRare(() -> wolf.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 2, STRENGTH_AMPLIFIER, true, false)))
                        .hasLegendary(() -> {
                            if (player.getHealth() < LOW_HEALTH_THRESHOLD) {
                                wolf.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 2, IMPROVED_STRENGTH_AMPLIFIER, true, false));
                                wolf.addEffect(new MobEffectInstance(MobEffects.SPEED, 2, IMPROVED_SPEED_AMPLIFIER, true, false));
                            }
                        });
                var wolfset = playerKill.get(player.getUUID());
                if (wolfset != null) {
                    if (!wolfset.contains(wolf)) {
                        wolfset.add(wolf);
                        wolf.setHealth(wolf.getHealth() + wolf.getMaxHealth() * HEAL_PERCENTAGE);
                        Helpers.debug("{} has the epic wolf card, healing {} by {} half hearts", player.getName().getString(), wolf.getName().getString(), HEAL_PERCENTAGE * wolf.getMaxHealth());
                    } else {
                        playerKill.remove(player.getUUID());
                    }
                }
            }
        }
    }

    @Override
    public void onPLayerKill(ServerPlayer player, Entity entity, DamageSource killingBlow) {
        if (PlayerData.hasCardOrRarer(player, cardType, RarityLevel.EPIC)) {
            playerKill.putIfAbsent(player.getUUID(), new HashSet<>());
        }
    }
}
