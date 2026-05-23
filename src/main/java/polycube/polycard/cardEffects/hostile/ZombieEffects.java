package polycube.polycard.cardEffects.hostile;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.events.callBacks.ItemConsumedEventCallback;
import polycube.polycard.manager.CardManager;
import polycube.polycard.utils.Helpers;

public class ZombieEffects {
    public static final CardType CARD_TYPE = CardType.ZOMBIE;

    public static final int STRENGTH_EFFECT_DURATION = 20 * 30;
    public static final int STRENGTH_EFFECT_AMPLIFIER = 0;

    public static final int REGENERATION_EFFECT_DURATION = 20 * 5;
    public static final int REGENERATION_EFFECT_AMPLIFIER = 0;

    public static void register(CardManager cardManager) {
        ItemConsumedEventCallback.EVENT.register((player, itemStack) -> onRottenFleshConsumed(cardManager, player, itemStack));
    }

    private static void onRottenFleshConsumed(CardManager cardManager, ServerPlayer player, ItemStack itemStack) {
        if (itemStack.is(Items.ROTTEN_FLESH)) {
            var playerData = cardManager.getStorage().data(player);
            if (playerData.hasCardOrRarer(CARD_TYPE, RarityLevel.COMMON)) {
                Helpers.runLater(0, _ -> player.removeEffect(MobEffects.HUNGER));

                if (playerData.hasCardOrRarer(CARD_TYPE, RarityLevel.UNCOMMON)) {
                    player.getFoodData().eat(2, 0);

                    if (playerData.hasCardOrRarer(CARD_TYPE, RarityLevel.RARE)) {
                        player.getFoodData().eat(2, 0);

                        if (playerData.hasCardOrRarer(CARD_TYPE, RarityLevel.EPIC)) {
                            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, STRENGTH_EFFECT_DURATION, STRENGTH_EFFECT_AMPLIFIER));

                            if (playerData.hasCardOrRarer(CARD_TYPE, RarityLevel.LEGENDARY)) {
                                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGENERATION_EFFECT_DURATION, REGENERATION_EFFECT_AMPLIFIER));
                            }
                        }
                    }
                }
            }
        }
    }
}
