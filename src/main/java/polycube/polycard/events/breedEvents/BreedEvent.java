package polycube.polycard.events.breedEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;

import java.util.Optional;

@FunctionalInterface
public interface BreedEvent {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    InteractionResult handle(ServerPlayer player, final Animal parent, final Animal partner, final Optional<AgeableMob> child);
}
