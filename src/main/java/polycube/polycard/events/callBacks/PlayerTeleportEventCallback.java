package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/// Fired after a server player was successfully teleported by an ender pearl, a chorus fruit or a card effect.
/// fromLevel and fromPosition describe where the player stood right before the teleport.
public interface PlayerTeleportEventCallback {
    Event<PlayerTeleportEventCallback> EVENT = EventFactory.createArrayBacked(PlayerTeleportEventCallback.class,
            (listeners) -> (player, cause, fromLevel, fromPosition) -> {
                for (var listener : listeners) {
                    listener.onPlayerTeleport(player, cause, fromLevel, fromPosition);
                }
            });

    void onPlayerTeleport(ServerPlayer player, TeleportCause cause, ServerLevel fromLevel, Vec3 fromPosition);

    enum TeleportCause {
        ENDER_PEARL,
        CHORUS_FRUIT,
        CARD_EFFECT
    }
}
