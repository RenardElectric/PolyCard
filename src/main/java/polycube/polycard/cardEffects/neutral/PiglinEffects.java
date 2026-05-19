package polycube.polycard.cardEffects.neutral;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.events.callBacks.IsTargetedEventCallback;
import polycube.polycard.manager.CardManager;

public class PiglinEffects {
    public static final CardType CARD_TYPE = CardType.PIGLIN;

    public static void register(CardManager cardManager) {
        IsTargetedEventCallback.EVENT.register(
                (level, targeter, target, targetingConditions, originalResult)
                        -> canBeTargeted(cardManager, level, targeter, target, targetingConditions, originalResult)
        );
    }

    private static InteractionResult canBeTargeted(CardManager cardManger, ServerLevel level, @Nullable LivingEntity targeter, LivingEntity target, TargetingConditions targetingConditions, boolean originalResult) {
        if (target instanceof Player player) {
            var playerData = cardManger.getStorage().data(player);
            switch (targeter) {
                case Piglin _ -> {
                    if (playerData.hasCardOrRarer(CARD_TYPE, RarityLevel.COMMON)) {
                        return InteractionResult.FAIL;
                    }
                }
                case PiglinBrute _ -> {
                    if (playerData.hasCardOrRarer(CARD_TYPE, RarityLevel.EPIC)) {
                        return InteractionResult.FAIL;
                    }
                }
                case null, default -> { }
            }
        }

        return InteractionResult.PASS;
    }
}
