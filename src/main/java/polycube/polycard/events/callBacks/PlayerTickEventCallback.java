package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/// Fired every tick for each player on the server.
public interface PlayerTickEventCallback {
    Event<PlayerTickEventCallback> EVENT = EventFactory.createArrayBacked(PlayerTickEventCallback.class,
            (listeners) -> (server, player) -> {
                for (var listener : listeners) {
                    listener.onPlayerTick(server, player);
                }
            });

    void onPlayerTick(MinecraftServer server, ServerPlayer player);
}
