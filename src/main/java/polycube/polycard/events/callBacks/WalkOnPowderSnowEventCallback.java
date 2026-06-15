package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;

/// Lets card effects override whether an entity can walk on powder snow.
/// PASS keeps the vanilla result, SUCCESS forces true, and FAIL forces false.
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
