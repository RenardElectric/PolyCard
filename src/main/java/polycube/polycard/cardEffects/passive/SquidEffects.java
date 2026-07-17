package polycube.polycard.cardEffects.passive;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.EntityAfterHurtEventCallback;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;
import polycube.polycard.utils.Helpers;

public class SquidEffects extends CardEffects implements PlayerTickEventCallback, EntityAfterHurtEventCallback {
    public static final float BLINDNESS_WHEN_HIT_PROBABILITY = 0.1f;
    public static final float BLINDNESS_ON_HIT_PROBABILITY = 0.1f;
    public static final int BLINDNESS_DURATION = 20 * 5;
    public static final int BLINDNESS_AMPLIFIER = 0;

    public static final int WATER_BREATHING_DURATION = 20 * 2;
    public static final int WATER_BREATHING_AMPLIFIER = 0;

    @Override
    public void onPlayerTick(MinecraftServer server, ServerPlayer player) {
        if (PlayerData.hasCardOrRarer(player, cardType(), RarityLevel.LEGENDARY)) {
            if (player.isEyeInFluid(FluidTags.WATER)) {
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, WATER_BREATHING_DURATION, WATER_BREATHING_AMPLIFIER, true, true), player);
            }
        }
    }

    @Override
    public void afterEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, float damageDealt) {
        var attacker = source.getEntity();
        if (entity instanceof ServerPlayer player
                && attacker instanceof LivingEntity livingAttacker
                && !attacker.equals(entity)
                && PlayerData.hasCardOrRarer(player, cardType(), RarityLevel.RARE)
                && level.getRandom().nextFloat() < BLINDNESS_WHEN_HIT_PROBABILITY) {
            Helpers.debug("{} triggered Rare Squid blindness against {}", player.getName().getString(), livingAttacker.getName().getString());
            livingAttacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION, BLINDNESS_AMPLIFIER), player);
        }

        if (attacker instanceof ServerPlayer sourcePlayer
                && !entity.equals(sourcePlayer)
                && PlayerData.hasCardOrRarer(sourcePlayer, cardType(), RarityLevel.EPIC)
                && level.getRandom().nextFloat() < BLINDNESS_ON_HIT_PROBABILITY) {
            Helpers.debug("{} triggered Epic Squid blindness against {}", sourcePlayer.getName().getString(), entity.getName().getString());
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION, BLINDNESS_AMPLIFIER), sourcePlayer);
        }
    }
}
