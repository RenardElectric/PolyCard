package polycube.polycard.events.cardEvents.neutral.enderMan;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.Rarity;
import polycube.polycard.events.callBacks.ItemUseEventCallback;
import polycube.polycard.events.callBacks.ProjectileOnHitEventCallback;
import polycube.polycard.manager.CardManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EnderManHandler {

    private static final int RESISTANCE_EFFECT_DURATION = 40;
    private static final int RESISTANCE_EFFECT_AMPLIFIER = 0;

    private static final List<ResourceKey<Biome>> endBiomes = new ArrayList<>(
            Arrays.asList(Biomes.THE_END, Biomes.END_BARRENS, Biomes.END_HIGHLANDS, Biomes.END_MIDLANDS, Biomes.SMALL_END_ISLANDS)
    );

    public static void registerEnderManCardEvents(CardManager cardManager) {
        ItemUseEventCallback.register((player, world, hand) -> onEnderPearlUsed(cardManager, player, world, hand));
        ProjectileOnHitEventCallback.EVENT.register((projectile, hitResult) -> onEnderPearlHit(cardManager, projectile, hitResult));
        ProjectileOnHitEventCallback.EVENT.register((projectile, hitResult) -> onProjectileHit(cardManager, projectile, hitResult));

        PolyCard.runTaskTimer(0, 20, server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                var pos = player.blockPosition();
                //noinspection resource
                var biome = player.level().getBiome(pos);
                if (biome.is(endBiomes::contains)) {
                    if (cardManager.getStorage().hasCardOrRarer(player, new Card(CardType.ENDERMAN, Rarity.LEGENDARY))) {
                        PolyCard.debug("Applying enderman card resistance effect to {}", player.getName().getString());
                        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, RESISTANCE_EFFECT_DURATION, RESISTANCE_EFFECT_AMPLIFIER, true, true));
                    }
                }
            }
        });
    }

    private static InteractionResult onEnderPearlUsed(CardManager cardManager, ServerPlayer player, Level world, InteractionHand hand) {
        var itemStack = player.getItemInHand(hand);
        if (itemStack.getItem() == Items.ENDER_PEARL) {
            if (cardManager.getStorage().hasCardOrRarer(player, new Card(CardType.ENDERMAN, Rarity.RARE))) {
                PolyCard.runLater(1, _ -> {
                    PolyCard.debug("Removing ender pearl cooldown for {}", player.getName().getString());
                    var cooldowns = player.getCooldowns();
                    cooldowns.removeCooldown(cooldowns.getCooldownGroup(Items.ENDER_PEARL.getDefaultInstance()));
                });

            }
        }

        return InteractionResult.PASS;
    }

    private static InteractionResult onEnderPearlHit(CardManager cardManager, Projectile projectile, HitResult hitResult) {
        if (projectile instanceof ThrownEnderpearl enderPearl) {
            var owner = enderPearl.getOwner();
            if (owner instanceof ServerPlayer player) {
                if (cardManager.getStorage().hasCardOrRarer(player, new Card(CardType.ENDERMAN, Rarity.RARE))) {
                    PolyCard.debug("{} has a rare or higher enderman card, removing ender pearl teleport damage", player.getName().getString());
                    var fireTicks = player.getRemainingFireTicks();
                    player.getAbilities().invulnerable = true;
                    PolyCard.runLater(1, _ -> {
                        player.getAbilities().invulnerable = false;
                        player.setRemainingFireTicks(fireTicks);
                    });
                }
            }
        }

        return InteractionResult.PASS;
    }

    private static InteractionResult onProjectileHit(CardManager cardManager, Projectile projectile, HitResult hitResult) {
        if (hitResult instanceof EntityHitResult entityHitResult) {
            var owner = entityHitResult.getEntity();
            if (owner instanceof ServerPlayer player) {
                if (cardManager.getStorage().hasCardOrRarer(player, new Card(CardType.ENDERMAN, Rarity.EPIC))) {
                    var random = ThreadLocalRandom.current();
                    var randomInt = random.nextInt(0, 100);
                    if (randomInt < 100) {
                        PolyCard.debug("{} has a epic or higher enderman card and rolled a {} to dodge a projectile", player.getName().getString(), randomInt);

                        //noinspection resource
                        var level = player.level();
                        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS);
                        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 0.5, player.getZ(), 50, 0.5, 1, 0.5, 1);

                        return InteractionResult.FAIL;
                    }
                }
            }
        }

        return InteractionResult.PASS;
    }
}
