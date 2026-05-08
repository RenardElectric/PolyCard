package polycube.polycard.events.breedEvents;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;

import java.util.Optional;

/// Callback for breeding two animals together.
/// Called after the two animals are bread.
/// Upon return:
/// - SUCCESS cancels further processing and continues with normal breeding behavior.
/// - PASS falls back to further processing and defaults to SUCCESS if no other listeners are available
public interface BreedEventCallback {
    Event<BreedEventCallback> EVENT = EventFactory.createArrayBacked(BreedEventCallback.class,
            (listeners) -> (player, parent, partner, child) -> {
                for (BreedEventCallback listener : listeners) {
                    InteractionResult result = listener.interact(player, parent, partner, child);

                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            });

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    InteractionResult interact(ServerPlayer player, Animal parent, Animal partner, Optional<AgeableMob> child);
}
