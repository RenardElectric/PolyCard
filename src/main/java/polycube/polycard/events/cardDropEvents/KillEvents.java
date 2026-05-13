package polycube.polycard.events.cardDropEvents;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.EnderMan;
import polycube.polycard.card.CardType;
import polycube.polycard.events.callBacks.KillEventCallback;
import polycube.polycard.manager.CardManager;


public class KillEvents {
    public static void registerKillEvents() {
        KillEventCallback.EVENT.register((player, entity, killingBlow) -> {

            switch (entity) {
                case IronGolem _ -> CardManager.receiveCard(player, CardType.IRON_GOLEM);
                case EnderMan _ -> CardManager.receiveCard(player, CardType.ENDERMAN);
                default -> { }
            }

            return InteractionResult.PASS;
        });
    }
}
