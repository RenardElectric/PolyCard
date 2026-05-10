package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;

/// Callback for a projectile hitting an entity or block.
/// Called after the hit result is determined but before any damage or block interaction is processed.
///
/// Upon return:
/// - SUCCESS cancels further processing and continues with normal hit effects (damage, block interaction, etc.)
/// - PASS falls back to further processing and defaults to SUCCESS if no other listeners are available
/// - FAIL cancels further processing and prevents any hit effects from occurring
public interface ProjectileOnHitEventCallback {
    Event<ProjectileOnHitEventCallback> EVENT = EventFactory.createArrayBacked(ProjectileOnHitEventCallback.class,
            (listeners) -> (projectile, hitResult) -> {
                for (ProjectileOnHitEventCallback listener : listeners) {
                    InteractionResult result = listener.interact(projectile, hitResult);

                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            });

    InteractionResult interact(Projectile projectile, HitResult hitResult);
}
