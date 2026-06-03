package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;

/// Callback for an entity walking on powder snow.
///
/// Upon return:
/// - SUCCESS cancels further processing and allows the entity to walk on the powder snow
/// - PASS falls back to further processing and defaults to the original result if no other listeners are available
/// - FAIL cancels further processing and prevents the entity from walking on the powder snow
public interface WalkOnPowderSnowEventCallback {
    Event<WalkOnPowderSnowEventCallback> EVENT = EventFactory.createArrayBacked(WalkOnPowderSnowEventCallback.class,
            (listeners) -> (entity, originalResult) -> {
                for (var listener : listeners) {
                    InteractionResult result = listener.interact(entity, originalResult);

                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            });

    InteractionResult interact(Entity entity, boolean originalResult);
}
