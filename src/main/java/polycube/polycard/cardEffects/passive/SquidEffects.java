package polycube.polycard.cardEffects.passive;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import polycube.polycard.PolyCard;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.manager.CardManager;

import java.util.concurrent.ThreadLocalRandom;

public class SquidEffects {
    public static final CardType CARD_TYPE = CardType.SQUID;

    public static final int BLINDNESS_WHEN_HIT_CHANCE = 10;
    public static final int BLINDNESS_ON_HIT_CHANCE = 10;
    public static final int BLINDNESS_DURATION = 100;
    public static final int BLINDNESS_AMPLIFIER = 0;

    public static final int WATER_BREATHING_DURATION = 221;
    public static final int WATER_BREATHING_AMPLIFIER = 0;

    public static void register(CardManager cardManager) {
        EntityHurtEventCallback.EVENT.register((attacker, level, source) -> onPlayerHurt(cardManager, attacker, level, source));

        PolyCard.runTaskTimer(0, 20, server -> {
            server.getPlayerList().getPlayers().forEach(player -> {
                if (cardManager.getStorage().data(player).hasCardOrRarer(CARD_TYPE, RarityLevel.LEGENDARY)) {
                    if (player.isEyeInFluid(FluidTags.WATER)) {
                        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, WATER_BREATHING_DURATION, WATER_BREATHING_AMPLIFIER, false, true), player);
                    }
                }
            });
        });
    }

    public static InteractionResult onPlayerHurt(CardManager cardManager, LivingEntity entity, ServerLevel level, DamageSource source) {
        if (entity instanceof ServerPlayer player && source.getEntity() instanceof ServerPlayer sourcePlayer) {
            if (cardManager.getStorage().data(player).hasCardOrRarer(CARD_TYPE, RarityLevel.RARE)) {
                int random = ThreadLocalRandom.current().nextInt(0, 100);
                if (random < BLINDNESS_WHEN_HIT_CHANCE) {
                    sourcePlayer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION, BLINDNESS_AMPLIFIER, false, true), player);
                }
            }

            if (cardManager.getStorage().data(sourcePlayer).hasCardOrRarer(CARD_TYPE, RarityLevel.EPIC)) {
                int random = ThreadLocalRandom.current().nextInt(0, 100);
                if (random < BLINDNESS_ON_HIT_CHANCE) {
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION, BLINDNESS_AMPLIFIER, false, true), sourcePlayer);
                }
            }
        }
        return InteractionResult.PASS;
    }

}
