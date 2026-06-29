package polycube.polycard.cardEffects.hostile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jspecify.annotations.Nullable;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.ExplosionKnockbackEventCallback;
import polycube.polycard.events.callBacks.IsTargetedEventCallback;
import polycube.polycard.utils.Helpers;

public class CreeperEffects {
    public static final CardType CARD_TYPE = CardType.CREEPER;

    public static final float EXPLOSION_DAMAGE_REDUCTION = 0.5f;
    public static final float EXPLOSION_KNOCKBACK_REDUCTION = 0.5f;
    public static final float EXPLOSION_PROBABILITY = 0.25f;
    public static final float EXPLOSION_RADIUS = 3f;

    public static void register() {
        EntityHurtEventCallback.EVENT.register(CreeperEffects::onEntityHurt);
        ExplosionKnockbackEventCallback.EVENT.register(CreeperEffects::explosionKnockbackReduction);
        IsTargetedEventCallback.EVENT.register(CreeperEffects::onTargeted);
    }

    private static InteractionResult onEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, MutableFloat damage) {
        if (entity instanceof ServerPlayer player) {
            if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.UNCOMMON)) {
                if (source.is(DamageTypes.PLAYER_EXPLOSION) || source.is(DamageTypes.EXPLOSION)) {
                    var damageReduction = damage.floatValue() * EXPLOSION_DAMAGE_REDUCTION;
                    Helpers.debug("{} has the uncommon creeper card and is taking explosion damage, reducing damage from {} to {}", player.getName().getString(), damage.floatValue(), damageReduction);
                    damage.setValue(damageReduction);
                }

                if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.LEGENDARY)) {
                    if (level.getRandom().nextFloat() < EXPLOSION_PROBABILITY) {
                        Helpers.debug("{} has the legendary creeper card and is took damage, triggering a small explosion", player.getName().getString());
                        level.explode(player, player.getX(), player.getY(), player.getZ(), EXPLOSION_RADIUS, false, Level.ExplosionInteraction.NONE);
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    private static float explosionKnockbackReduction(Entity entity, float knockback) {
        if (entity instanceof ServerPlayer player) {
            if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.RARE)) {
                var knockbackReduction = knockback * EXPLOSION_KNOCKBACK_REDUCTION;
                Helpers.debug("{} has the rare creeper card and is taking explosion knockback, reducing knockback from {} to {}", player.getName().getString(), knockback, knockbackReduction);
                return knockbackReduction;
            }
        }
        return knockback;
    }

    private static InteractionResult onTargeted(ServerLevel level, @Nullable LivingEntity targeter, LivingEntity target, IsTargetedEventCallback.TargetingConditionsData data) {
        if (target instanceof ServerPlayer player) {
            if (targeter instanceof Creeper) {
                if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.LEGENDARY)) {
                    return InteractionResult.FAIL;
                }
            }
        }
        return InteractionResult.PASS;
    }
}

