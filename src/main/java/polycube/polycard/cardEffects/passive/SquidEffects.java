package polycube.polycard.cardEffects.passive;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.mutable.MutableFloat;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.utils.Helpers;

public class SquidEffects {
    public static final CardType CARD_TYPE = CardType.SQUID;

    public static final float BLINDNESS_WHEN_HIT_PROBABILITY = 0.1f;
    public static final float BLINDNESS_ON_HIT_PROBABILITY = 0.1f;
    public static final int BLINDNESS_DURATION = 20 * 5;
    public static final int BLINDNESS_AMPLIFIER = 0;

    public static final int WATER_BREATHING_DURATION = 20 * 2;
    public static final int WATER_BREATHING_AMPLIFIER = 0;

    public static void register() {
        EntityHurtEventCallback.EVENT.register(SquidEffects::onPlayerHurt);

        Helpers.addPlayerTask((server, player) -> {
            if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.LEGENDARY)) {
                if (player.isEyeInFluid(FluidTags.WATER)) {
                    player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, WATER_BREATHING_DURATION, WATER_BREATHING_AMPLIFIER, true, true), player);
                }
            }
        });
    }

    private static InteractionResult onPlayerHurt(LivingEntity entity, ServerLevel level, DamageSource source, MutableFloat damage) {
        if (entity instanceof ServerPlayer player && source.getEntity() instanceof ServerPlayer sourcePlayer) {
            if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.RARE)) {
                double random = level.getRandom().nextFloat();
                if (random < BLINDNESS_WHEN_HIT_PROBABILITY) {
                    Helpers.debug("{} has the rare squid card and got hit by {}. Applying blindness.", player.getName().getString(), sourcePlayer.getName().getString());
                    sourcePlayer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION, BLINDNESS_AMPLIFIER), player);
                }
            }

            if (PlayerData.hasCardOrRarer(sourcePlayer, CARD_TYPE, RarityLevel.EPIC)) {
                double random = level.getRandom().nextFloat();
                if (random < BLINDNESS_ON_HIT_PROBABILITY) {
                    Helpers.debug("{} has the epic squid card and hit {}. Applying blindness.", sourcePlayer.getName().getString(), player.getName().getString());
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION, BLINDNESS_AMPLIFIER), sourcePlayer);
                }
            }
        }
        return InteractionResult.PASS;
    }

}
