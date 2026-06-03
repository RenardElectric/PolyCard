package polycube.polycard.cardEffects.neutral;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.WalkOnPowderSnowEventCallback;

public class GoatEffects {
    public static final CardType CARD_TYPE = CardType.GOAT;

    public static void register() {
        WalkOnPowderSnowEventCallback.EVENT.register(GoatEffects::onWalkOnPowderSnow);
    }

    private static InteractionResult onWalkOnPowderSnow(Entity entity, boolean originalResult) {
         if (!originalResult) {
            if (entity instanceof ServerPlayer player) {
                if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.UNCOMMON)) {
                    return InteractionResult.SUCCESS;  // TODO: Does noe work
                }
            }
        }
        return InteractionResult.PASS;
    }
}
