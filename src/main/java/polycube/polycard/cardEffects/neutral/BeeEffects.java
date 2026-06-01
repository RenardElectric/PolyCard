package polycube.polycard.cardEffects.neutral;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import polycube.polycard.card.CardType;
import polycube.polycard.events.callBacks.ItemConsumedEventCallback;
import polycube.polycard.manager.CardManager;
import polycube.polycard.utils.CardRarityConditions;
import polycube.polycard.utils.Helpers;

public class BeeEffects {
    public static final CardType CARD_TYPE = CardType.BEE;

    public static final int SPEED_EFFECT_DURATION = 20 * 60;
    public static final int SPEED_EFFECT_AMPLIFIER = 0;

    public static final int REGENERATION_EFFECT_DURATION = 20 * 10;
    public static final int REGENERATION_EFFECT_AMPLIFIER = 0;

    public static final int HEALTH_BOOST_EFFECT_DURATION = 20 * 60;
    public static final int HEALTH_BOOST_EFFECT_AMPLIFIER = 1;

    public static void register(CardManager cardManager) {
        ItemConsumedEventCallback.EVENT.register((player, itemStack) -> onHoneyBottleConsumed(cardManager, player, itemStack));
    }

    private static void onHoneyBottleConsumed(CardManager cardManager, ServerPlayer player, ItemStack itemStack) {
        if (itemStack.is(Items.HONEY_BOTTLE)) {
            var playerName = player.getDisplayName().getString();
            CardRarityConditions.of(cardManager, player, CARD_TYPE)
                    .hasRare(() -> {
                        Helpers.debug("{} has the rare bee card and consumed a honey bottle. Applying speed.", playerName);
                        player.addEffect(new MobEffectInstance(MobEffects.SPEED, SPEED_EFFECT_DURATION, SPEED_EFFECT_AMPLIFIER));
                    })
                    .hasEpic(() -> {
                        Helpers.debug("{} has the epic bee card and consumed a honey bottle. Applying regeneration.", playerName);
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGENERATION_EFFECT_DURATION, REGENERATION_EFFECT_AMPLIFIER));
                    })
                    .hasLegendary(() -> {
                        Helpers.debug("{} has the legendary bee card and consumed a honey bottle. Applying health boost.", playerName);
                        player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, HEALTH_BOOST_EFFECT_DURATION, HEALTH_BOOST_EFFECT_AMPLIFIER));
                    });
        }
    }
}
