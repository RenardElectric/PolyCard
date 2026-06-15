package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/// Fired when Minecraft's killed-entity advancement trigger runs.
/// The return value only controls later PolyCard listeners; it does not cancel the kill.
public interface KillEventCallback {
    Event<KillEventCallback> EVENT = EventFactory.createArrayBacked(KillEventCallback.class,
            (listeners) -> (player, entity, killingBlow) -> {
                for (var listener : listeners) {
                    listener.interact(player, entity, killingBlow);
                }
            });

    void interact(ServerPlayer player, Entity entity, DamageSource killingBlow);
}
