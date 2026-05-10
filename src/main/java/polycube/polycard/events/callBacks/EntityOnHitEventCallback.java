package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;

/// Callback for an entity being hit.
/// Called before the hit damage is applied.
///
/// Upon return:
/// - SUCCESS cancels further processing and continues with normal hit effects (e.g. damage, armor reduction, enchantments, etc.)
/// - PASS falls back to further processing and defaults to SUCCESS if no other listeners are available
/// - FAIL cancels further processing and prevents any hit damage from being applied
public interface EntityOnHitEventCallback {
    Event<EntityOnHitEventCallback> EVENT = EventFactory.createArrayBacked(EntityOnHitEventCallback.class,
            (listeners) -> (instance, level, source) -> {
                for (EntityOnHitEventCallback listener : listeners) {
                    InteractionResult result = listener.interact(instance, level, source);

                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            });

    InteractionResult interact(LivingEntity instance, ServerLevel level, DamageSource source);
}
