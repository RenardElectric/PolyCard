package polycube.polycard.cardEffects.passive;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.manager.CardManager;
import polycube.polycard.utils.CardRarityConditions;
import polycube.polycard.utils.Helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChickenEffects {
    public static final CardType CARD_TYPE = CardType.CHICKEN;

    public static final int SPEED_EFFECT_DURATION = 20 * 2;
    public static final int SPEED_EFFECT_AMPLIFIER = 0;
    public static final int SPEED_DISTANCE_SQUARED = 25;

    public static void register(CardManager cardManager) {
        EntityHurtEventCallback.EVENT.register(
                (entity, level, source) -> playerHit(cardManager, entity, level, source)
        );

        Map<UUID, Integer> eggTimes = new HashMap<>();
        Helpers.addPlayerTask((server, player) -> {
            var uuid = player.getUUID();

            CardRarityConditions.of(cardManager, player, CARD_TYPE)
                    .hasUncommon(() -> {
                        var eggTime = eggTimes.computeIfAbsent(uuid, _ -> player.getRandom().nextInt(6000) + 6000);
                        if (--eggTime < 0) {
                            if (player.dropFromGiftLootTable(player.level(), BuiltInLootTables.CHICKEN_LAY, player::spawnAtLocation)) {
                                player.playSound(SoundEvents.CHICKEN_EGG, 1.0F, (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.2F + 1.0F);
                                eggTime = player.getRandom().nextInt(6000) + 6000;
                            }
                        }
                        eggTimes.put(uuid, eggTime);
                    }, () -> eggTimes.remove(uuid))
                    .hasRare(() -> {
                        if (Helpers.nearPlayerWithCard(player, cardManager, CARD_TYPE, RarityLevel.RARE, SPEED_DISTANCE_SQUARED)) {
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

    private static InteractionResult playerHit(CardManager cardManager, LivingEntity entity, ServerLevel level, DamageSource source) {
        if (entity instanceof ServerPlayer player) {
            if (cardManager.getStorage().get(player).hasCardOrRarer(CARD_TYPE, RarityLevel.EPIC)) {
                var pos = source.getSourcePosition();
                var attacker = source.getDirectEntity();
                if (attacker != null) {
                    if (attacker instanceof Projectile arrow) {
                        var owner = arrow.getOwner();
                        if (owner != null) {
                            attacker = owner;
                        }
                    }
                    pos = attacker.position();
                }
                if (pos != null) {
                    ItemStack projectile = new ItemStack(Items.EGG);
                    var thrownEgg = new ThrownEgg(level, player, projectile);
                    double xd = pos.x() - player.getX();
                    double yd = pos.y() - thrownEgg.getY();
                    double zd = pos.z() - player.getZ();
                    double distanceToTarget = Math.sqrt(xd * xd + zd * zd);
                    Projectile.spawnProjectileUsingShoot(
                            thrownEgg, level, projectile, xd, yd + distanceToTarget * 0.2F, zd, (float) distanceToTarget * 0.06F, 0
                    );
                    level.playSound(
                            null, player.getX(), player.getY(), player.getZ(), SoundEvents.EGG_THROW, SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
                    );
                }
            }
        }
        return InteractionResult.PASS;
    }
}
