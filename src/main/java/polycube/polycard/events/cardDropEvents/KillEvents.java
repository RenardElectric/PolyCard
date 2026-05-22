package polycube.polycard.events.cardDropEvents;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.piglin.Piglin;
import polycube.polycard.card.CardType;
import polycube.polycard.events.callBacks.KillEventCallback;
import polycube.polycard.manager.CardManager;


public class KillEvents {
    public static void register() {
        KillEventCallback.EVENT.register((player, entity, killingBlow) -> {

            switch (entity) {
                case EnderMan _ -> CardManager.receiveCard(player, CardType.ENDERMAN);
                case Squid _ -> CardManager.receiveCard(player, CardType.SQUID);
                case Piglin _ -> CardManager.receiveCard(player, CardType.PIGLIN);
                default -> { }
            }

            return InteractionResult.PASS;
        });
    }
}
