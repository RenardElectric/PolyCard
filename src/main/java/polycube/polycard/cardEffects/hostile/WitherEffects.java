package polycube.polycard.cardEffects.hostile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.mutable.MutableFloat;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.KillEventCallback;
import polycube.polycard.utils.CardRarityConditions;
import polycube.polycard.utils.Helpers;

public class WitherEffects {
    public static final CardType CARD_TYPE = CardType.WITHER;

    public static final float WITHER_ROSE_DROP_PROBABILITY = 0.25f;

    public static final float WITHER_EFFECT_PROBABILITY = 0.2f;
    public static final int WITHER_EFFECT_DURATION = 20 * 5;
    public static final int WITHER_EFFECT_AMPLIFIER = 0;

    public static final float DAMAGE_INCREASE_PROBABILITY = 0.2f;

    public static final float LIFE_STEAL_PROBABILITY = 0.1f;

    public static void register() {
        KillEventCallback.EVENT.register(WitherEffects::onKill);
        EntityHurtEventCallback.EVENT.register(WitherEffects::onHurt);
    }

    private static void onKill(ServerPlayer player, Entity entity, DamageSource damageSource) {
        if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.COMMON)) {
            if (player.getRandom().nextFloat() < WITHER_ROSE_DROP_PROBABILITY) {
                Helpers.debug("{} has the common wither card and killed {}, dropping a wither rose with a probability of {}", player.getName().getString(), entity.getName().getString(), WITHER_ROSE_DROP_PROBABILITY);
                entity.spawnAtLocation(player.level(), Items.WITHER_ROSE);
            }
        }
    }

    private static InteractionResult onHurt(LivingEntity entity, ServerLevel level, DamageSource source, MutableFloat damage) {
        if (entity instanceof ServerPlayer player) {
            if (source.is(DamageTypes.WITHER) && PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.UNCOMMON)) {
                return InteractionResult.FAIL;
            }
        }

        var attacker = source.getEntity();
        if (attacker instanceof ServerPlayer player) {
            var random = player.getRandom();
            CardRarityConditions.of(player, CARD_TYPE)
                    .hasRare(() -> {
                        if (random.nextFloat() < WITHER_EFFECT_PROBABILITY)
                            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_EFFECT_DURATION, WITHER_EFFECT_AMPLIFIER, false, true), player);
                    })
                    .hasEpic(() -> {
                        if (entity.hasEffect(MobEffects.WITHER)) {
                            int damageIncrease = 0;
                            for (int i = 0; i < damage.floatValue(); i++) {
                                if (random.nextFloat() < DAMAGE_INCREASE_PROBABILITY) damageIncrease++;
                            }
                            damage.setValue(damage.floatValue() + damageIncrease);
                        }
                    })
                    .hasLegendary(() -> {
                        if (entity.hasEffect(MobEffects.WITHER)) {
                            int life = 0;
                            for (int i = 0; i < damage.floatValue(); i++) {
                                if (random.nextFloat() < LIFE_STEAL_PROBABILITY) life++;
                            }
                            player.heal(life);
                        }
                    });
        }

        return InteractionResult.PASS;
    }
}
