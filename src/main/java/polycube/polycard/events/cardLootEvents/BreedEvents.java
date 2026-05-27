package polycube.polycard.events.cardLootEvents;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import polycube.polycard.card.CardType;
import polycube.polycard.events.callBacks.BreedEventCallback;
import polycube.polycard.manager.CardManager;

public class BreedEvents {
    public static void register() {
        BreedEventCallback.EVENT.register((player, parent, partner, child) -> {

            switch (parent) {
                case Cow _ -> CardManager.receiveCard(player, CardType.COW);
                case Chicken _ -> CardManager.receiveCard(player, CardType.CHICKEN);
                default -> { }
            }

            return InteractionResult.PASS;
        });
    }
}
