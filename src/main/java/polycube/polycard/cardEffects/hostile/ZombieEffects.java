package polycube.polycard.cardEffects.hostile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.camel.CamelHusk;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilus;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.IsTargetedEventCallback;
import polycube.polycard.events.callBacks.ItemConsumedEventCallback;
import polycube.polycard.utils.CardRarityConditions;
import polycube.polycard.utils.Helpers;

public class ZombieEffects extends CardEffects implements ItemConsumedEventCallback, IsTargetedEventCallback {
    public static final int STRENGTH_EFFECT_DURATION = 20 * 30;
    public static final int STRENGTH_EFFECT_AMPLIFIER = 0;

    public static final int REGENERATION_EFFECT_DURATION = 20 * 5;
    public static final int REGENERATION_EFFECT_AMPLIFIER = 0;

    public static final int ROTTEN_FLESH_FOOD_INCREASE = 2;

    @Override
    public void onItemConsumed(ServerPlayer player, ItemStack itemStack) {
        if (itemStack.is(Items.ROTTEN_FLESH)) {
            CardRarityConditions.of(player, cardType)
                    .hasCommon(() -> {
                        Helpers.debug("{} has the common zombie card and consumed rotten flesh, removing hunger effect", player.getName().getString());
                        Helpers.runLater(0, _ -> player.removeEffect(MobEffects.HUNGER));
                    })
                    .hasUncommon(() -> {
                        Helpers.debug("{} has the uncommon zombie card and consumed rotten flesh, adding 2 hunger points", player.getName().getString());
                        player.getFoodData().eat(ROTTEN_FLESH_FOOD_INCREASE, 0);
                    })
                    .hasEpic(() -> {
                        Helpers.debug("{} has the rare zombie card and consumed rotten flesh, adding 2 hunger points", player.getName().getString());
                        player.getFoodData().eat(ROTTEN_FLESH_FOOD_INCREASE, 0);
                    })
                    .hasLegendary(() -> {
                        Helpers.debug("{} has the epic zombie card and consumed rotten flesh, adding strength effect", player.getName().getString());
                        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, STRENGTH_EFFECT_DURATION, STRENGTH_EFFECT_AMPLIFIER));
                    });
        }
    }

    @Override
    public InteractionResult onTargeted(ServerLevel level, @Nullable LivingEntity targeter, LivingEntity target, IsTargetedEventCallback.TargetingConditionsData data) {
        if (target instanceof ServerPlayer player) {
            if (targeter instanceof Zombie || targeter instanceof ZombieHorse || targeter instanceof ZombieNautilus || targeter instanceof CamelHusk || targeter instanceof Zoglin) {
                if (PlayerData.hasCardOrRarer(player, cardType, RarityLevel.LEGENDARY)) {
                    return InteractionResult.FAIL;
                }
            }
        }
        return InteractionResult.PASS;
    }
}
