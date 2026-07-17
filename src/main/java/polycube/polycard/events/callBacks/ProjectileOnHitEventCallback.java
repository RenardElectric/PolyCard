package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;

/// Fired after a server-side projectile has a hit result but before Projectile.onHit continues.
/// PASS allows vanilla hit handling; any non-PASS result consumes the projectile and cancels onHit.
public interface ProjectileOnHitEventCallback {
    Event<ProjectileOnHitEventCallback> EVENT = EventFactory.createArrayBacked(ProjectileOnHitEventCallback.class,
            (listeners) -> (projectile, hitResult) -> {
                for (var listener : listeners) {
                    InteractionResult result = listener.onProjectileHit(projectile, hitResult);

                    if (!result.equals(InteractionResult.PASS)) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            });

    InteractionResult onProjectileHit(Projectile projectile, HitResult hitResult);
}
