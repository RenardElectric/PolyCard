package polycube.polycard.cardEffects.hostile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jspecify.annotations.Nullable;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.EntityAfterHurtEventCallback;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.ExplosionKnockbackEventCallback;
import polycube.polycard.events.callBacks.IsTargetedEventCallback;
import polycube.polycard.utils.Helpers;

public class CreeperEffects
        extends CardEffects
        implements EntityHurtEventCallback, EntityAfterHurtEventCallback,
        ExplosionKnockbackEventCallback, IsTargetedEventCallback {
    public static final float EXPLOSION_DAMAGE_REDUCTION = 0.5f;
    public static final float EXPLOSION_KNOCKBACK_REDUCTION = 0.5f;
    public static final float EXPLOSION_PROBABILITY = 0.25f;
    public static final float EXPLOSION_RADIUS = 1.5f;

    private boolean ignoreExplosion = false;

    @Override
    public InteractionResult onEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, MutableFloat damage) {
        if (entity instanceof ServerPlayer player
                && PlayerData.hasCardOrRarer(player, cardType(), RarityLevel.UNCOMMON)
                && source.is(DamageTypeTags.IS_EXPLOSION)) {
            var damageReduction = damage.floatValue() * EXPLOSION_DAMAGE_REDUCTION;
            Helpers.debug("{} has the uncommon creeper card and is taking explosion damage, reducing damage from {} to {}", player.getName().getString(), damage.floatValue(), damageReduction);
            damage.setValue(damageReduction);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void afterEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, float damageDealt) {
        if (entity instanceof ServerPlayer player
                && !ignoreExplosion
                && PlayerData.hasCardOrRarer(player, cardType(), RarityLevel.EPIC)
                && player.getRandom().nextFloat() < EXPLOSION_PROBABILITY) {
            Helpers.debug("{} triggered an Epic Creeper-card blast after taking an accepted hit", player.getName().getString());
            ignoreExplosion = true;
            try {
                level.explode(player, player.getX(), player.getY(), player.getZ(), EXPLOSION_RADIUS, false, Level.ExplosionInteraction.NONE);
            } finally {
                ignoreExplosion = false;
            }
        }
    }

    @Override
    public float onExplosionKnockback(Entity entity, float knockback) {
        if (entity instanceof ServerPlayer player) {
            if (PlayerData.hasCardOrRarer(player, cardType(), RarityLevel.RARE)) {
                var knockbackReduction = knockback * EXPLOSION_KNOCKBACK_REDUCTION;
                Helpers.debug("{} has the rare creeper card and is taking explosion knockback, reducing knockback from {} to {}", player.getName().getString(), knockback, knockbackReduction);
                return knockbackReduction;
            }
        }
        return knockback;
    }

    @Override
    public InteractionResult onTargeted(ServerLevel level, @Nullable LivingEntity targeter, LivingEntity target, IsTargetedEventCallback.TargetingConditionsData data) {
        if (target instanceof ServerPlayer player) {
            if (targeter instanceof Creeper) {
                if (PlayerData.hasCardOrRarer(player, cardType(), RarityLevel.LEGENDARY)) {
                    return InteractionResult.FAIL;
                }
            }
        }
        return InteractionResult.PASS;
    }
}

