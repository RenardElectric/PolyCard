package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;

/// Fired when an entity is about to receive knockback from an explosion.
/// This event allows you to modify the knockback value before it is applied to the entity.
public interface ExplosionKnockbackEventCallback {
    Event<ExplosionKnockbackEventCallback> EVENT = EventFactory.createArrayBacked(ExplosionKnockbackEventCallback.class,
            (listeners) -> (entity, knockback) -> {
                for (var listener : listeners) {
                    knockback = listener.knockback(entity, knockback);
                }
                return knockback;
            });

    float knockback(Entity entity, float knockback);
}
