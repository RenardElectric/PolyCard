package polycube.polycard.events.cardLootEvents;

import net.minecraft.world.entity.animal.equine.Horse;
import polycube.polycard.card.CardType;
import polycube.polycard.events.callBacks.TameEventCallback;
import polycube.polycard.utils.CardHelper;

public class TameEvents {
    public static void register() {
        TameEventCallback.EVENT.register((player, animal) -> {
            switch (animal) {
                case Horse _ -> CardHelper.receiveCard(player, CardType.HORSE);
                default -> { }
            }
        });
    }
}
