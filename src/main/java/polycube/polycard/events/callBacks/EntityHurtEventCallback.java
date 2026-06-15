package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.mutable.MutableFloat;

/// Fired before server-side hurt damage is applied.
/// Listeners may mutate damage; the first non-PASS result stops later PolyCard listeners.
/// FAIL cancels the damage, while SUCCESS keeps vanilla hurt processing with the current damage value.
public interface EntityHurtEventCallback {
    Event<EntityHurtEventCallback> EVENT = EventFactory.createArrayBacked(EntityHurtEventCallback.class,
            (listeners) -> (instance, level, source, damage) -> {
                for (var listener : listeners) {
                    InteractionResult result = listener.interact(instance, level, source, damage);

                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            });

    InteractionResult interact(LivingEntity instance, ServerLevel level, DamageSource source, MutableFloat damage);
}
