package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.mutable.MutableFloat;

/// Fired before server-side hurt damage is applied.
/// Listeners may mutate damage; FAIL cancels it, while PASS and SUCCESS allow later listeners.
public interface EntityHurtEventCallback {
    Event<EntityHurtEventCallback> EVENT = EventFactory.createArrayBacked(EntityHurtEventCallback.class,
            (listeners) -> (instance, level, source, damage) -> {
                for (var listener : listeners) {
                    InteractionResult result = listener.onEntityHurt(instance, level, source, damage);

                    if (result.equals(InteractionResult.FAIL)) {
                        return InteractionResult.FAIL;
                    }
                }

                return InteractionResult.PASS;
            });

    InteractionResult onEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, MutableFloat damage);
}
