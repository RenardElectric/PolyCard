package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;

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
