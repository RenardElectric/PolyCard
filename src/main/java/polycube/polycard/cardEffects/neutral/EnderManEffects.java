package polycube.polycard.cardEffects.neutral;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.ItemUseEventCallback;
import polycube.polycard.events.callBacks.ProjectileOnHitEventCallback;
import polycube.polycard.utils.Helpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EnderManEffects {
    public static final CardType CARD_TYPE = CardType.ENDERMAN;

    public static final int RESISTANCE_EFFECT_DURATION = 20 * 2;
    public static final int RESISTANCE_EFFECT_AMPLIFIER = 0;

    public static final int PROJECTILE_DODGE_PROBABILITY = 20;

    public static final List<ResourceKey<Biome>> endBiomes = new ArrayList<>(
            Arrays.asList(Biomes.THE_END, Biomes.END_BARRENS, Biomes.END_HIGHLANDS, Biomes.END_MIDLANDS, Biomes.SMALL_END_ISLANDS)
    );

    public static void register() {
        ItemUseEventCallback.register(EnderManEffects::onEnderPearlUsed);
        ProjectileOnHitEventCallback.EVENT.register(EnderManEffects::onProjectileHit);
        EntityHurtEventCallback.EVENT.register(EnderManEffects::onEnderPearlHit);

        Helpers.addPlayerTask((server, player) -> {
            var pos = player.blockPosition();
            //noinspection resource
            var biome = player.level().getBiome(pos);
            if (biome.is(endBiomes::contains)) {
                if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.LEGENDARY)) {
                    player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, RESISTANCE_EFFECT_DURATION, RESISTANCE_EFFECT_AMPLIFIER, true, true));
                }
            }
        });
    }

    private static InteractionResult onEnderPearlUsed(ServerPlayer player, Level world, InteractionHand hand) {
        var itemStack = player.getItemInHand(hand);
        if (itemStack.getItem() == Items.ENDER_PEARL) {
            if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.RARE)) {
                Helpers.runLater(1, _ -> {
                    Helpers.debug("Removing ender pearl cooldown for {}", player.getName().getString());
                    var cooldowns = player.getCooldowns();
                    cooldowns.removeCooldown(cooldowns.getCooldownGroup(Items.ENDER_PEARL.getDefaultInstance()));
                });
            }
        }

        return InteractionResult.PASS;
    }

    private static InteractionResult onEnderPearlHit(LivingEntity entity, ServerLevel level, DamageSource source, EntityHurtEventCallback.AtomicDouble damage) {
        if (entity instanceof ServerPlayer player) {
            if (source.is(DamageTypes.ENDER_PEARL)) {
                if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.UNCOMMON)) {
                    Helpers.debug("{} has an uncommon or higher enderman card, removing ender pearl teleport damage", player.getName().getString());
                    return InteractionResult.FAIL;
                }
            }
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult onProjectileHit(Projectile projectile, HitResult hitResult) {
        if (hitResult instanceof EntityHitResult entityHitResult) {
            var hitEntity = entityHitResult.getEntity();
            if (hitEntity instanceof ServerPlayer player) {
                if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.EPIC)) {
                    var random = ThreadLocalRandom.current();
                    var randomInt = random.nextInt(0, 100);
                    if (randomInt < PROJECTILE_DODGE_PROBABILITY) {
                        Helpers.debug("{} has a epic or higher enderman card and rolled a {} to dodge a projectile", player.getName().getString(), randomInt);

                        //noinspection resource
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
