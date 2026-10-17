package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

/// Lets card effects transform the launch power of an ender pearl thrown by a player.
/// Listeners run in order, each receiving the power returned by the previous listener.
public interface EnderPearlThrowEventCallback {
    Event<EnderPearlThrowEventCallback> EVENT = EventFactory.createArrayBacked(EnderPearlThrowEventCallback.class,
            (listeners) -> (player, power) -> {
                for (var listener : listeners) {
                    power = listener.onEnderPearlThrow(player, power);
                }
                return power;
            });

    float onEnderPearlThrow(ServerPlayer player, float power);
}
