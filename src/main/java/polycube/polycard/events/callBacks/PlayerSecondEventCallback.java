package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/// Fired every second for each player on the server.
public interface PlayerSecondEventCallback {
    Event<PlayerSecondEventCallback> EVENT = EventFactory.createArrayBacked(PlayerSecondEventCallback.class,
            (listeners) -> (server, player) -> {
                for (var listener : listeners) {
                    listener.onPlayerSecond(server, player);
                }
            });

    void onPlayerSecond(MinecraftServer server, ServerPlayer player);
}
