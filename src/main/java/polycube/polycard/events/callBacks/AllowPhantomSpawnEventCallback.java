package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public interface AllowPhantomSpawnEventCallback {
    Event<AllowPhantomSpawnEventCallback> EVENT = EventFactory.createArrayBacked(AllowPhantomSpawnEventCallback.class,
            (listeners) -> (player, level) -> {
                for (var listener : listeners) {
                    if (!listener.allowPhantomSpawn(player, level)) {
                        return false;
                    }
                }
                return true;
            });

    boolean allowPhantomSpawn(ServerPlayer player, ServerLevel level);
}
