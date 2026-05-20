package polycube.polycard.events.cardDropEvents;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.IronGolem;
import polycube.polycard.card.CardType;
import polycube.polycard.events.callBacks.EntitySummonedEventCallback;
import polycube.polycard.manager.CardManager;

public class SummonEvents {
    public static void register() {
        EntitySummonedEventCallback.EVENT.register((player, entity) -> {

            switch (entity) {
                case IronGolem _ -> CardManager.receiveCard(player, CardType.IRON_GOLEM);
                default -> { }
            }

            return InteractionResult.PASS;
        });
    }
}
