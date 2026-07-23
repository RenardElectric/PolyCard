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
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.EntityAfterHurtEventCallback;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.PlayerKillEventCallback;
import polycube.polycard.utils.CardRarityConditions;
import polycube.polycard.utils.Helpers;

public class WitherEffects extends CardEffects implements PlayerKillEventCallback, EntityHurtEventCallback, EntityAfterHurtEventCallback {
    public static final float WITHER_ROSE_DROP_PROBABILITY = 0.25f;

    public static final float WITHER_EFFECT_PROBABILITY = 0.2f;
    public static final int WITHER_EFFECT_DURATION = 20 * 5;
    public static final int WITHER_EFFECT_AMPLIFIER = 0;

    public static final float DAMAGE_INCREASE_PROBABILITY = 0.2f;

    public static final float LIFE_STEAL_PROBABILITY = 0.1f;

    @Override
    public void onPlayerKill(ServerPlayer player, Entity entity, DamageSource killingBlow) {
        if (hasCardOrRarer(player, RarityLevel.COMMON)) {
            if (player.getRandom().nextFloat() < WITHER_ROSE_DROP_PROBABILITY) {
                Helpers.debug("{} dropped a Wither Rose from {} with a Common Wither card ({}% chance)",
                        player.getName().getString(), entity.getName().getString(), Helpers.probToStr(WITHER_ROSE_DROP_PROBABILITY));
                entity.spawnAtLocation(player.level(), Items.WITHER_ROSE);
            }
        }
    }

    @Override
    public InteractionResult onEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, MutableFloat damage) {
        if (entity instanceof ServerPlayer player) {
            if (source.is(DamageTypes.WITHER) && hasCardOrRarer(player, RarityLevel.UNCOMMON)) {
                return InteractionResult.FAIL;
            }
        }

        var attacker = source.getEntity();
        if (attacker instanceof ServerPlayer player) {
            CardRarityConditions.of(player, cardType())
                    .hasEpic(() -> {
                        if (entity.hasEffect(MobEffects.WITHER)) {
                            int damageIncrease = Helpers.binomialSelection(DAMAGE_INCREASE_PROBABILITY, damage.intValue());
                            if (damageIncrease > 0) {
                                damage.setValue(damage.floatValue() + damageIncrease);
                                Helpers.debug("{} gained {} Wither-card bonus damage against {}", player.getName().getString(), damageIncrease, entity.getName().getString());
                            }
                        }
                    });
        }

        return InteractionResult.PASS;
    }

    @Override
    public void afterEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, float damageDealt) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (damageDealt > 0.0F
                && entity.hasEffect(MobEffects.WITHER)
                && hasCardOrRarer(player, RarityLevel.LEGENDARY)) {
            int life = Helpers.binomialSelection(LIFE_STEAL_PROBABILITY, (int) damageDealt);
            if (life > 0) {
                player.heal(life);
                Helpers.debug("{} healed {} health from {} actual Wither-card damage", player.getName().getString(), life, damageDealt);
            }
        }

        if (hasCardOrRarer(player, RarityLevel.RARE) && player.getRandom().nextFloat() < WITHER_EFFECT_PROBABILITY) {
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_EFFECT_DURATION, WITHER_EFFECT_AMPLIFIER, false, true), player);
            Helpers.debug("{} inflicted Wither on {} after an accepted hit", player.getName().getString(), entity.getName().getString());
        }
    }
}
