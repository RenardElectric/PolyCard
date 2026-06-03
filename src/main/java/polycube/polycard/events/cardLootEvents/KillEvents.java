package polycube.polycard.events.cardLootEvents;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.zombie.Zombie;
import polycube.polycard.card.CardType;
import polycube.polycard.events.callBacks.KillEventCallback;
import polycube.polycard.utils.CardHelper;


public class KillEvents {
    public static void register() {
        KillEventCallback.EVENT.register((player, entity, killingBlow) -> {

            switch (entity) {
                case EnderMan _ -> CardHelper.receiveCard(player, CardType.ENDERMAN);
                case Squid _ -> CardHelper.receiveCard(player, CardType.SQUID);
                case Piglin _ -> CardHelper.receiveCard(player, CardType.PIGLIN);
                case Zombie _  -> CardHelper.receiveCard(player, CardType.ZOMBIE);
                default -> { }
            }

            return InteractionResult.PASS;
        });
    }
}
