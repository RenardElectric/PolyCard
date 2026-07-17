package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/// Fired after hurtServer accepts a hit and vanilla finishes applying it, including fatal hits.
/// damageDealt is the health plus absorption actually removed by that call.
public interface EntityAfterHurtEventCallback {
    Event<EntityAfterHurtEventCallback> EVENT = EventFactory.createArrayBacked(
            EntityAfterHurtEventCallback.class,
            listeners -> (entity, level, source, damageDealt) -> {
                for (var listener : listeners) {
                    listener.afterEntityHurt(entity, level, source, damageDealt);
                }
            }
    );

    void afterEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, float damageDealt);
}
