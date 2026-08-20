package polycube.polycard.cardEffects.passive;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import polycube.polycard.PolyCard;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.EntityAfterHurtEventCallback;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;
import polycube.polycard.utils.EffectHelpers;

public class SquidEffects extends CardEffects implements PlayerTickEventCallback, EntityAfterHurtEventCallback {
    public static final float BLINDNESS_WHEN_HIT_PROBABILITY = 0.1f;
    public static final float BLINDNESS_ON_HIT_PROBABILITY = 0.1f;
    public static final int BLINDNESS_DURATION = 20 * 5;
    public static final int BLINDNESS_AMPLIFIER = 0;

    public static final int WATER_BREATHING_AMPLIFIER = 0;

    @Override
    public void onPlayerTick(MinecraftServer server, ServerPlayer player) {
        if (hasCardOrRarer(player, RarityLevel.LEGENDARY)) {
            if (player.isUnderWater()) {
                EffectHelpers.refreshPersistentEffect(player, MobEffects.WATER_BREATHING, WATER_BREATHING_AMPLIFIER);
            }
        }
    }

    @Override
    public void afterEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, float damageDealt) {
        var attacker = source.getEntity();
        if (entity instanceof ServerPlayer player
                && attacker instanceof LivingEntity livingAttacker
                && !attacker.equals(entity)
                && hasCardOrRarer(player, RarityLevel.RARE)
                && level.getRandom().nextFloat() < BLINDNESS_WHEN_HIT_PROBABILITY) {
            PolyCard.LOGGER.debug("{} triggered Rare Squid blindness against {}", player.getName().getString(), livingAttacker.getName().getString());
            livingAttacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION, BLINDNESS_AMPLIFIER), player);
        }

        if (attacker instanceof ServerPlayer sourcePlayer
                && !entity.equals(sourcePlayer)
                && hasCardOrRarer(sourcePlayer, RarityLevel.EPIC)
                && level.getRandom().nextFloat() < BLINDNESS_ON_HIT_PROBABILITY) {
            PolyCard.LOGGER.debug("{} triggered Epic Squid blindness against {}", sourcePlayer.getName().getString(), entity.getName().getString());
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION, BLINDNESS_AMPLIFIER), sourcePlayer);
        }
    }
}
