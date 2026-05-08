package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/// Callback for killing an entity.
/// Called after the entity is killed.
/// Upon return:
/// - SUCCESS cancels further processing and continues with normal killing behavior.
/// - PASS falls back to further processing and defaults to SUCCESS if no other listeners are available
public interface KillEventCallback {
    Event<KillEventCallback> EVENT = EventFactory.createArrayBacked(KillEventCallback.class,
            (listeners) -> (player, entity, killingBlow) -> {
                for (KillEventCallback listener : listeners) {
                    InteractionResult result = listener.interact(player, entity, killingBlow);

                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            });

    InteractionResult interact(ServerPlayer player, Entity entity, DamageSource killingBlow);
}
