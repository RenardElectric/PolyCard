package polycube.polycard.cardEffects.passive;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.utils.CardRarityConditions;
import polycube.polycard.utils.Helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChickenEffects {
    public static final CardType CARD_TYPE = CardType.CHICKEN;

    public static final float EGG_DAMAGE = 1.0F;

    public static final int SPEED_EFFECT_DURATION = 20 * 2;
    public static final int SPEED_EFFECT_AMPLIFIER = 0;
    public static final int SPEED_DISTANCE_SQUARED = 25;

    public static void register() {
        EntityHurtEventCallback.EVENT.register(
                ChickenEffects::onHurt
        );

        Map<UUID, Integer> eggTimes = new HashMap<>();
        Helpers.addPlayerTask((server, player) -> {
            var uuid = player.getUUID();

            CardRarityConditions.of(player, CARD_TYPE)
                    .hasCommon(() -> {
                        var eggTime = eggTimes.computeIfAbsent(uuid, _ -> player.getRandom().nextInt(6000) + 6000);
                        if (--eggTime < 0) {
                            var level = player.level();
                            var chicken = EntityType.CHICKEN.create(level, EntitySpawnReason.TRIGGERED);
                            if (chicken != null) {
                                chicken.remove(Entity.RemovalReason.DISCARDED);
                                if (chicken.dropFromGiftLootTable(level, BuiltInLootTables.CHICKEN_LAY, player::spawnAtLocation)) {
                                    level.playSound(null, player.getX(), player.getY() - 1, player.getZ(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 0.2f, (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.2F + 1.0F);
                                    eggTime = player.getRandom().nextInt(6000) + 6000;
                                }
                            }
                        }
                        eggTimes.put(uuid, eggTime);
                    }, () -> eggTimes.remove(uuid))
                    .hasRare(() -> {
                        if (Helpers.nearPlayerWithCard(player, CARD_TYPE, RarityLevel.RARE, SPEED_DISTANCE_SQUARED)) {
                            player.addEffect(new MobEffectInstance(MobEffects.SPEED, SPEED_EFFECT_DURATION, SPEED_EFFECT_AMPLIFIER, true, true));
                        }
                    })
                    .hasLegendary(() -> {
                        if (player.fallDistance > 2 && player.isCrouching()) {
                            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 5, 0, false, true, true));
                        }
                    });
        });
    }

    private static boolean ignore = false;

    private static InteractionResult onHurt(LivingEntity entity, ServerLevel level, DamageSource source) {
        if (ignore) return InteractionResult.PASS;

        if (source.getDirectEntity() instanceof ThrownEgg egg) {
            if (egg.getOwner() instanceof ServerPlayer player) {
                if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.UNCOMMON)) {
                    ignore = true;
                    entity.hurtServer(level, egg.damageSources().thrown(egg, player), EGG_DAMAGE);
                    ignore = false;
                }
            }
        }

        if (entity instanceof ServerPlayer player) {
            if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.EPIC)) {
                var pos = source.getSourcePosition();
                var attacker = source.getDirectEntity();
                if (attacker != null) {
                    if (attacker instanceof Projectile projectile) {
                        var owner = projectile.getOwner();
                        if (owner != null) {
                            attacker = owner;
                        }
                    }
                    pos = attacker.position();
                }
                if (pos != null) {
                    ItemStack eggStack = new ItemStack(Items.EGG);
                    ThrownEgg egg = new ThrownEgg(level, player, eggStack);
                    Vec3 eggAim = getProjectileAim(
                            egg,
                            pos.x(),
                            pos.y() + 0.5,
                            pos.z()
                    );
                    if (eggAim != null) {
                        Projectile.spawnProjectileUsingShoot(
                                egg,
                                level,
                                eggStack,
                                eggAim.x,
                                eggAim.y,
                                eggAim.z,
                                (float) eggAim.length(),
                                0.0F
                        );
                    }
                    level.playSound(
                            null, player.getX(), player.getY(), player.getZ(), SoundEvents.EGG_THROW,
                            SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
                    );
                }
            }
        }
        return InteractionResult.PASS;
    }

    private static Vec3 getProjectileAim(
            Projectile projectile,
            double targetX,
            double targetY,
            double targetZ
    ) {
        double dx = targetX - projectile.getX();
        double dy = targetY - projectile.getY();
        double dz = targetZ - projectile.getZ();

        double h = Math.sqrt(dx * dx + dz * dz);
        if (h < 1.0e-6) {
            return null;
        }

        double angle = Math.atan2(dy, h);
        angle += (Math.PI / 2.0 - 0.01 - angle) * 0.25;

        double cos = Math.cos(angle);
        if (cos <= 1.0e-6) {
            return null;
        }

        double inertia = projectile.isInWater() ? 0.8 : 0.99;
        double gravity = projectile.getGravity();
        double tan = Math.tan(angle);

        double bestSpeed = Double.NaN;
        double bestError = Double.POSITIVE_INFINITY;

        for (int tick = 1; tick <= 200; tick++) {
            double dragSum = inertia * (1.0 - Math.pow(inertia, tick)) / (1.0 - inertia);
            double drop = gravity * inertia / (1.0 - inertia) * (tick - dragSum);
            double error = Math.abs(h * tan - drop - dy);

            if (error < bestError) {
                bestError = error;
                bestSpeed = h / (cos * dragSum);
            }
        }

        if (Double.isNaN(bestSpeed)) {
            return null;
        }

        return new Vec3(
                dx / h * cos * bestSpeed,
                Math.sin(angle) * bestSpeed,
                dz / h * cos * bestSpeed
        );
    }
}
