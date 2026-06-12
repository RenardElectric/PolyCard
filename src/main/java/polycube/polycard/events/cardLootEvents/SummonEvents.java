package polycube.polycard.events.cardLootEvents;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import polycube.polycard.card.CardType;
import polycube.polycard.events.callBacks.EntitySummonedEventCallback;
import polycube.polycard.utils.CardHelper;

public class SummonEvents {
    public static void register() {
        EntitySummonedEventCallback.EVENT.register((player, entity) -> {

            switch (entity) {
                case IronGolem _ -> CardHelper.receiveCard(player, CardType.IRON_GOLEM);
                case WitherBoss _ -> CardHelper.receiveCard(player, CardType.WITHER);
                case EnderDragon _ -> CardHelper.receiveCard(player, CardType.ENDER_DRAGON);
                case CopperGolem _, SnowGolem _ -> { }
                default -> { }
            }

            return InteractionResult.PASS;
        });
    }
}
