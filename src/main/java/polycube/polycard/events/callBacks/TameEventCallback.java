package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;

/// Fired when Minecraft's tamed-animals advancement trigger runs.
public interface TameEventCallback {
    Event<TameEventCallback> EVENT = EventFactory.createArrayBacked(TameEventCallback.class,
            (listeners) -> (player, animal) -> {
                for (var listener : listeners) {
                    listener.onTame(player, animal);
                }
            });

    void onTame(ServerPlayer player, Animal animal);
}
