package polycube.polycard.events.killEvents;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

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

    InteractionResult interact(final ServerPlayer player, final Entity entity, final DamageSource killingBlow);
}
