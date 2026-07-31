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
import polycube.polycard.PolyCard;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.PlayerKillEventCallback;
import polycube.polycard.utils.Helpers;

import java.util.Objects;

public final class WolfEffects extends CardEffects implements PlayerKillEventCallback, ServerTickEvents.EndLevelTick {

    public static final int RESISTANCE_AMPLIFIER = 0;
    public static final int STRENGTH_AMPLIFIER = 0;
    public static final int LOW_HEALTH_THRESHOLD = 8;
    public static final int IMPROVED_STRENGTH_AMPLIFIER = 1;
    public static final int IMPROVED_SPEED_AMPLIFIER = 1;
    public static final float HEAL_PERCENTAGE = 0.25f;
    private static final int BUFF_REFRESH_INTERVAL = 20;
    private static final int BUFF_DURATION = BUFF_REFRESH_INTERVAL + 5;

    @Override
    public void onEndTick(ServerLevel level) {
        if (level.getGameTime() % BUFF_REFRESH_INTERVAL != 0) {
            return;
        }

        for (var wolf : level.getEntities(EntityTypes.WOLF, LivingEntity::isAlive)) {
            if (wolf.getRootOwner() instanceof ServerPlayer player) {
                var rarity = PolyCard.storage().getPlayerData(player).equippedRarityLevel(cardType());
                if (rarity == null) {
                    continue;
                }
                if (rarity.isAtLeast(RarityLevel.UNCOMMON)) {
                    wolf.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, BUFF_DURATION, RESISTANCE_AMPLIFIER, true, false));
                }
                if (rarity.isAtLeast(RarityLevel.RARE)) {
                    int strengthAmplifier = rarity.isAtLeast(RarityLevel.LEGENDARY) && player.getHealth() < LOW_HEALTH_THRESHOLD
                            ? IMPROVED_STRENGTH_AMPLIFIER
                            : STRENGTH_AMPLIFIER;
                    wolf.addEffect(new MobEffectInstance(MobEffects.STRENGTH, BUFF_DURATION, strengthAmplifier, true, false));
                }
                if (rarity.isAtLeast(RarityLevel.LEGENDARY) && player.getHealth() < LOW_HEALTH_THRESHOLD) {
                    wolf.addEffect(new MobEffectInstance(MobEffects.SPEED, BUFF_DURATION, IMPROVED_SPEED_AMPLIFIER, true, false));
                }
            }
        }
    }

    @Override
    public void onPlayerKill(ServerPlayer player, Entity entity, DamageSource killingBlow) {
        if (hasCardOrRarer(player, RarityLevel.EPIC)) {
            int healedWolves = 0;
            for (var level : player.level().getServer().getAllLevels()) {
                for (var wolf : level.getEntities(EntityTypes.WOLF, LivingEntity::isAlive)) {
                    if (Objects.equals(wolf.getRootOwner(), player)) {
                        wolf.heal(wolf.getMaxHealth() * HEAL_PERCENTAGE);
                        healedWolves++;
                    }
                }
            }
            if (healedWolves > 0) {
                Helpers.debug("{} healed {} owned wolf/wolves after a kill", player.getName().getString(), healedWolves);
            }
        }
    }
}
