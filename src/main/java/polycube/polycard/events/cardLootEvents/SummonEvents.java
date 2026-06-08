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
                case CopperGolem _ -> CardHelper.receiveCard(player, CardType.COPPER_GOLEM);
                case SnowGolem _ -> CardHelper.receiveCard(player, CardType.SNOW_GOLEM);
                case WitherBoss _ -> CardHelper.receiveCard(player, CardType.WITHER);
                case EnderDragon _ -> CardHelper.receiveCard(player, CardType.ENDER_DRAGON);
                default -> { }
            }

            return InteractionResult.PASS;
        });
    }
}
