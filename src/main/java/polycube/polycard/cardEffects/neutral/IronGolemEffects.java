package polycube.polycard.cardEffects.neutral;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.item.Items;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.manager.CardManager;
import polycube.polycard.utils.CardRarityConditions;
import polycube.polycard.utils.Helpers;

import java.util.concurrent.ThreadLocalRandom;

public class IronGolemEffects {
    public static final CardType CARD_TYPE = CardType.IRON_GOLEM;

    public static final int RESISTANCE_ON_ATTACKED_CHANCE = 20;
    public static final int RESISTANCE_DURATION = 20 * 10;
    public static final int RESISTANCE_AMPLIFIER = 0;

    public static final String KNOCKBACK_HIT_COOLDOWN_KEY = "iron_golem:knockback";
    public static final int KNOCKBACK_HIT_COOLDOWN = 20 * 10;
    public static final float KNOCKBACK_POWER = 3;

    public static final String SHOCKWAVE_COOLDOWN_KEY = "iron_golem:shockwave";
    public static final float SHOCKWAVE_MIN_FALL_DISTANCE = 4.0f;
    public static final int SHOCKWAVE_COOLDOWN = 200;
    public static final int MAX_SHOCKWAVE_DAMAGE = 15;

    public static void register(CardManager cardManager) {
        EntityHurtEventCallback.EVENT.register((attacker, level, source) -> onPlayerHurt(cardManager, attacker, level, source));
    }

    public static InteractionResult onPlayerHurt(CardManager cardManager, LivingEntity entity, ServerLevel level, DamageSource source) {

        if (entity instanceof ServerPlayer player) {
            CardRarityConditions.of(cardManager, player, CARD_TYPE)
                    .hasRare(() -> {
                        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
                            Helpers.debug("{} has a rare or higher Iron Golem card and was attacked. Chance to gain resistance: {}%", player.getName().getString(), RESISTANCE_ON_ATTACKED_CHANCE);
                            int random = ThreadLocalRandom.current().nextInt(0, 100);
                            if (random < RESISTANCE_ON_ATTACKED_CHANCE) {
                                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, RESISTANCE_DURATION, RESISTANCE_AMPLIFIER));
                            }
                        }
                    })
                    .hasLegendary(() -> {
                        if (source.is(DamageTypeTags.IS_FALL) &&
                                player.fallDistance >= SHOCKWAVE_MIN_FALL_DISTANCE &&
                                cardManager.getCooldowns().isReadyOrCreate(player, SHOCKWAVE_COOLDOWN_KEY, SHOCKWAVE_COOLDOWN)
                        ) {
                            Helpers.debug("{} has a legendary or higher Iron Golem card and fell from a height of {}. Triggering shockwave.", player.getName().getString(), player.fallDistance);
                            triggerShockwave(player, player.fallDistance);
                        }
                    });
        }

        if (source.getEntity() instanceof ServerPlayer player) {
            if (source.isDirect() && source.getWeaponItem() != null && source.getWeaponItem().is(Items.AIR)) {
                if (cardManager.getStorage().get(player).hasCardOrRarer(CARD_TYPE, RarityLevel.EPIC)) {
                    if (cardManager.getCooldowns().isReadyOrCreate(player, KNOCKBACK_HIT_COOLDOWN_KEY, KNOCKBACK_HIT_COOLDOWN)) {
                        Helpers.debug("{} has an epic or higher Iron Golem card, applying knockback on hit", player.getName().getString());
                        double xd = 0.0;
                        double zd = 0.0;
                        if (source.getSourcePosition() != null) {
                            xd = source.getSourcePosition().x() - entity.getX();
                            zd = source.getSourcePosition().z() - entity.getZ();
                        }
                        entity.knockback(KNOCKBACK_POWER, xd, zd);
                        Helpers.playSound(level, SoundEvents.MACE_SMASH_AIR, entity.position());
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY() + 1, entity.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
                    }
                }
            }
        }

        return InteractionResult.PASS;
    }

    private static void triggerShockwave(ServerPlayer player, double fallDistance) {
        var center = player.position();
        double radius = Math.min(6.0, 3.5 + Math.max(0.0, (fallDistance - SHOCKWAVE_MIN_FALL_DISTANCE) * 0.25));
        double maxDamage = Math.min(fallDistance / 2, MAX_SHOCKWAVE_DAMAGE);

        var level = player.level();
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 30, radius * 0.45, 0.2, radius * 0.45, 0.05);
        Helpers.playSound(level, SoundEvents.MACE_SMASH_GROUND_HEAVY, center);

        var condition = TargetingConditions.forNonCombat().ignoreLineOfSight().ignoreInvisibilityTesting().range(radius);
        for (LivingEntity nearby : level.getNearbyEntities(LivingEntity.class, condition, player, player.getBoundingBox().inflate(radius, 2.5, radius))) {
            if (nearby.equals(player)) continue;

            nearby.knockback(
                    0.8 + Math.min(0.8, fallDistance * 0.04),
                    player.getX() - nearby.getX(),
                    player.getZ() - nearby.getZ()
            );

            double squaredDistance = nearby.position().subtract(center).lengthSqr();
            nearby.hurtServer(level, player.damageSources().playerAttack(player), (float) (maxDamage * (1 - Math.min(0.9, squaredDistance / (radius * radius)))));
        }
    }
}
