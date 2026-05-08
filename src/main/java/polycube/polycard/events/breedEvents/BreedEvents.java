package polycube.polycard.events.breedEvents;

import net.minecraft.world.InteractionResult;

public class BreedEvents {
    public static void registerBreedEvents(BreedEvent... breedEvent) {
        BreedEventCallback.EVENT.register((player, parent, partner, child) -> {
            for (BreedEvent event : breedEvent) {
                var result = event.handle(player, parent, partner, child);
                if (result ==  InteractionResult.SUCCESS) {
                    return InteractionResult.SUCCESS;
                } else if (result == InteractionResult.FAIL) {
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });
    }
}
