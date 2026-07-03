package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;

import java.util.Optional;

/// Fired when Minecraft's bred-animals advancement trigger runs.
public interface BreedEventCallback {
    Event<BreedEventCallback> EVENT = EventFactory.createArrayBacked(BreedEventCallback.class,
            (listeners) -> (player, parent, partner, child) -> {
                for (var listener : listeners) {
                    listener.onBreed(player, parent, partner, child);
                }
            });

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void onBreed(ServerPlayer player, Animal parent, Animal partner, Optional<AgeableMob> child);
}
