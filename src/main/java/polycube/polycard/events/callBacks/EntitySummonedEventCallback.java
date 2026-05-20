package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;

public interface EntitySummonedEventCallback {
    Event<EntitySummonedEventCallback> EVENT = EventFactory.createArrayBacked(EntitySummonedEventCallback.class,
            (listeners) -> (player, entity) -> {
                for (var listener : listeners) {
                    InteractionResult result = listener.interact(player, entity);

                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            });

    InteractionResult interact(ServerPlayer player, Entity entity);
}
