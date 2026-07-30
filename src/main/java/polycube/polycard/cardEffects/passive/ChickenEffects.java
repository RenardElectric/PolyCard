package polycube.polycard.cardEffects.passive;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jspecify.annotations.Nullable;
import polycube.polycard.PolyCard;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.EntityAfterHurtEventCallback;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;
import polycube.polycard.utils.EffectHelpers;
import polycube.polycard.utils.Helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChickenEffects extends CardEffects implements PlayerTickEventCallback, EntityHurtEventCallback,
        EntityAfterHurtEventCallback, ServerPlayerEvents.Leave {
    public static final float EGG_DAMAGE = 1.0F;

    public static final int SPEED_EFFECT_AMPLIFIER = 0;
    public static final int SPEED_DISTANCE_SQUARED = 25;

    public static final int SLOW_FALLING_AMPLIFIER = 0;

    public static final float THROW_EGG_PROBABILITY = 0.5f;

    private static final Map<UUID, Integer> EGG_TIMES = new HashMap<>();

    @Override
    public void onPlayerTick(MinecraftServer server, ServerPlayer player) {
        var uuid = player.getUUID();
        var equippedRarity = PolyCard.storage().getPlayerData(player).equippedRarityLevel(cardType());
        if (equippedRarity == null) {
            EGG_TIMES.remove(uuid);
            return;
        }

        var eggTime = EGG_TIMES.computeIfAbsent(uuid, _ -> nextEggDelay(player));
        if (--eggTime <= 0) {
            eggTime = nextEggDelay(player);
            var level = player.level();
            var chicken = EntityTypes.CHICKEN.create(level, EntitySpawnReason.TRIGGERED);
            if (chicken != null) {
                chicken.setPos(player.position());
                try {
                    if (chicken.dropFromGiftLootTable(level, BuiltInLootTables.CHICKEN_LAY, player::spawnAtLocation)) {
                        Helpers.debug("{} laid an egg from a Common Chicken card", player.getName().getString());
                        level.playSound(null, player.getX(), player.getY() - 1, player.getZ(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 0.2f, (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.2F + 1.0F);
                    }
                } finally {
                    chicken.remove(Entity.RemovalReason.DISCARDED);
                }
            }
        }
        EGG_TIMES.put(uuid, eggTime);

        if (equippedRarity.isAtLeast(RarityLevel.RARE) && Helpers.nearPlayerWithCard(player, cardType(), RarityLevel.RARE, SPEED_DISTANCE_SQUARED)) {
            EffectHelpers.refreshPersistentEffect(player, MobEffects.SPEED, SPEED_EFFECT_AMPLIFIER);
        }

        if (equippedRarity.isAtLeast(RarityLevel.LEGENDARY) && player.fallDistance > 2 && player.isCrouching()) {
            EffectHelpers.refreshPersistentEffect(player, MobEffects.SLOW_FALLING, SLOW_FALLING_AMPLIFIER);
        }
    }

    private static int nextEggDelay(ServerPlayer player) {
        return player.getRandom().nextInt(6000) + 6000;
    }

    @Override
    public void onLeave(ServerPlayer player) {
        EGG_TIMES.remove(player.getUUID());
    }

    @Override
    public InteractionResult onEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, MutableFloat damage) {
        if (source.getDirectEntity() instanceof ThrownEgg egg) {
            if (egg.getOwner() instanceof ServerPlayer player) {
                if (hasCardOrRarer(player, RarityLevel.UNCOMMON)) {
                    Helpers.debug("{} dealt {} damage to {} with an Uncommon Chicken egg", player.getName().getString(), EGG_DAMAGE, entity.getName().getString());
                    damage.setValue(EGG_DAMAGE);
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void afterEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, float damageDealt) {
        if (!(entity instanceof ServerPlayer player)
                || !hasCardOrRarer(player, RarityLevel.EPIC)
                || player.getRandom().nextFloat() >= THROW_EGG_PROBABILITY) {
            return;
        }

        var pos = source.getSourcePosition();
        var attacker = source.getEntity();
        if (attacker != null) {
            pos = attacker.position();
        }
        if (pos == null) {
            return;
        }

        ItemStack eggStack = new ItemStack(Items.EGG);
        ThrownEgg egg = new ThrownEgg(level, player, eggStack);
        Vec3 eggAim = getProjectileAim(egg, pos.x(), pos.y() + 0.5, pos.z());
        if (eggAim == null) {
            return;
        }

        Helpers.debug("{} is retaliating with an Epic Chicken egg against {}",
                player.getName().getString(), attacker != null ? attacker.getName().getString() : "position " + pos);
        Projectile.spawnProjectileUsingShoot(
                egg, level, eggStack, eggAim.x, eggAim.y, eggAim.z,
                (float) eggAim.length(), 0.0F
        );
        level.playSound(
                null, player.getX(), player.getY(), player.getZ(), SoundEvents.EGG_THROW,
                SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
        );
    }

    private static @Nullable Vec3 getProjectileAim(
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
