package polycube.polycard.cardEffects.neutral;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.apache.commons.lang3.mutable.MutableFloat;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.ItemUseEventCallback;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;
import polycube.polycard.events.callBacks.ProjectileOnHitEventCallback;
import polycube.polycard.utils.EffectHelpers;
import polycube.polycard.utils.Helpers;

public final class EnderManEffects extends CardEffects implements PlayerTickEventCallback, ItemUseEventCallback, ProjectileOnHitEventCallback, EntityHurtEventCallback {
    public static final int RESISTANCE_EFFECT_AMPLIFIER = 0;

    public static final int PROJECTILE_DODGE_PROBABILITY = 20;

    @Override
    public void onPlayerTick(MinecraftServer server, ServerPlayer player) {
        if (player.level().dimension().equals(Level.END) && hasCardOrRarer(player, RarityLevel.UNCOMMON)) {
            EffectHelpers.refreshPersistentEffect(player, MobEffects.RESISTANCE, RESISTANCE_EFFECT_AMPLIFIER);
        }
    }

    @Override
    public InteractionResult onItemUse(ServerPlayer player, Level world, InteractionHand hand) {
        var itemStack = player.getItemInHand(hand);
        if (itemStack.getItem().equals(Items.ENDER_PEARL)) {
            if (hasCardOrRarer(player, RarityLevel.EPIC)) {
                Helpers.runLater(0, _ -> {
                    Helpers.debug("Removing ender pearl cooldown for {}", player.getName().getString());
                    var cooldowns = player.getCooldowns();
                    cooldowns.removeCooldown(cooldowns.getCooldownGroup(Items.ENDER_PEARL.getDefaultInstance()));
                });
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, MutableFloat damage) {
        if (entity instanceof ServerPlayer player) {
            if (source.is(DamageTypes.ENDER_PEARL)) {
                if (hasCardOrRarer(player, RarityLevel.RARE)) {
                    Helpers.debug("{} ignored ender-pearl damage with a Rare Enderman card", player.getName().getString());
                    return InteractionResult.FAIL;
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onProjectileHit(Projectile projectile, HitResult hitResult) {
        if (hitResult instanceof EntityHitResult entityHitResult) {
            var hitEntity = entityHitResult.getEntity();
            if (hitEntity instanceof ServerPlayer player) {
                if (hasCardOrRarer(player, RarityLevel.LEGENDARY)) {
                    var randomInt = player.getRandom().nextInt(100);
                    if (randomInt < PROJECTILE_DODGE_PROBABILITY) {
                        Helpers.debug("{} dodged a projectile with a Legendary Enderman card (roll {})", player.getName().getString(), randomInt);

                        var level = player.level();
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS);
                        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 0.5, player.getZ(), 50, 0.5, 1, 0.5, 1);

                        return InteractionResult.FAIL;
                    }
                }
            }
        }

        return InteractionResult.PASS;
    }
}
