package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;

/// Fired after a projectile has a hit result but before Projectile.onHit continues.
/// PASS allows vanilla hit handling; any non-PASS result cancels the original onHit method.
public interface ProjectileOnHitEventCallback {
    Event<ProjectileOnHitEventCallback> EVENT = EventFactory.createArrayBacked(ProjectileOnHitEventCallback.class,
            (listeners) -> (projectile, hitResult) -> {
                for (var listener : listeners) {
                    InteractionResult result = listener.interact(projectile, hitResult);

                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            });

    InteractionResult interact(Projectile projectile, HitResult hitResult);
}
