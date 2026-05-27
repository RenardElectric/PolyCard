package polycube.polycard.cardEffects.passive;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.manager.CardManager;

public class ChickenEffects {
    public static final CardType CARD_TYPE = CardType.CHICKEN;

    public static void register(CardManager cardManager) {
        EntityHurtEventCallback.EVENT.register(
                (entity, level, source) -> playerHit(cardManager, entity, level, source)
        );
    }

    private static InteractionResult playerHit(CardManager cardManager, LivingEntity entity, ServerLevel level, DamageSource source) {
        if (entity instanceof ServerPlayer player) {
            if (cardManager.getStorage().data(player).hasCardOrRarer(CARD_TYPE, RarityLevel.EPIC)) {
                var pos = source.getSourcePosition();
                var targetBHeight = source.getDirectEntity() != null ? 0 : 0;
                if (pos != null) {
                    ItemStack projectile = new ItemStack(Items.EGG);
                    var thrownEgg = new ThrownEgg(level, player, projectile);
                    double xd = pos.x() - player.getX();
                    double yd = pos.y() + targetBHeight * (0.3333333333333333) - thrownEgg.getY();
                    double zd = pos.z() - player.getZ();
                    double distanceToTarget = Math.sqrt(xd * xd + zd * zd);
                    Projectile.spawnProjectileUsingShoot(
                            thrownEgg, level, projectile, xd, yd + distanceToTarget * 0.2F, zd, 1.6F, 0
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
