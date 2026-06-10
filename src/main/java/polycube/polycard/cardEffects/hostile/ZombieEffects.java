package polycube.polycard.cardEffects.hostile;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import polycube.polycard.card.CardType;
import polycube.polycard.events.callBacks.ItemConsumedEventCallback;
import polycube.polycard.utils.CardRarityConditions;
import polycube.polycard.utils.Helpers;

public class ZombieEffects {
    public static final CardType CARD_TYPE = CardType.ZOMBIE;

    public static final int STRENGTH_EFFECT_DURATION = 20 * 30;
    public static final int STRENGTH_EFFECT_AMPLIFIER = 0;

    public static final int REGENERATION_EFFECT_DURATION = 20 * 5;
    public static final int REGENERATION_EFFECT_AMPLIFIER = 0;

    public static final int ROTTEN_FLESH_FOOD_INCREASE = 2;

    public static void register() {
        ItemConsumedEventCallback.EVENT.register(ZombieEffects::onRottenFleshConsumed);
    }

    private static void onRottenFleshConsumed(ServerPlayer player, ItemStack itemStack) {
        if (itemStack.is(Items.ROTTEN_FLESH)) {
            CardRarityConditions.of(player, CARD_TYPE)
                    .hasCommon(() -> {
                        Helpers.debug("{} has the common zombie card and consumed rotten flesh, removing hunger effect", player.getName().getString());
                        Helpers.runLater(0, _ -> player.removeEffect(MobEffects.HUNGER));
                    })
                    .hasUncommon(() -> {
                        Helpers.debug("{} has the uncommon zombie card and consumed rotten flesh, adding 2 hunger points", player.getName().getString());
                        player.getFoodData().eat(ROTTEN_FLESH_FOOD_INCREASE, 0);
                    })
                    .hasRare(() -> {
                        Helpers.debug("{} has the rare zombie card and consumed rotten flesh, adding 2 hunger points", player.getName().getString());
                        player.getFoodData().eat(ROTTEN_FLESH_FOOD_INCREASE, 0);
                    })
                    .hasEpic(() -> {
                        Helpers.debug("{} has the epic zombie card and consumed rotten flesh, adding strength effect", player.getName().getString());
                        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, STRENGTH_EFFECT_DURATION, STRENGTH_EFFECT_AMPLIFIER));
                    })
                    .hasLegendary(() -> {
                        Helpers.debug("{} has the legendary zombie card and consumed rotten flesh, adding regeneration effect", player.getName().getString());
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGENERATION_EFFECT_DURATION, REGENERATION_EFFECT_AMPLIFIER));
                    });
        }
    }
}
