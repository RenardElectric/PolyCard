package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

public interface PlayerLoadEventCallback {
    Event<PlayerLoadEventCallback> JOIN = EventFactory.createArrayBacked(PlayerLoadEventCallback.class,
            callbacks -> player -> {
        for (var callback : callbacks) {
            callback.interact(player);
        }
    });

    void interact(ServerPlayer player);
}
