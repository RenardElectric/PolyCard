package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/// Fired when Minecraft's summoned-entity advancement trigger runs.
public interface EntitySummonedEventCallback {
    Event<EntitySummonedEventCallback> EVENT = EventFactory.createArrayBacked(EntitySummonedEventCallback.class,
            (listeners) -> (player, entity) -> {
                for (var listener : listeners) {
                    listener.interact(player, entity);
                }
            });

    void interact(ServerPlayer player, Entity entity);
}
