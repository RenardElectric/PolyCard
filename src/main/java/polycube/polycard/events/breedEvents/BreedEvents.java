package polycube.polycard.events.breedEvents;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.cow.Cow;
import polycube.polycard.card.CardType;
import polycube.polycard.manager.CardManager;

public class BreedEvents {
    public static void registerBreedEvents() {
        BreedEventCallback.EVENT.register((player, parent, partner, child) -> {

            switch (parent) {
                case Cow _ -> CardManager.giveCard(player, CardType.COW);
                default -> { }
            }

            return InteractionResult.PASS;
        });
    }
}
