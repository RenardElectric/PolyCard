package polycube.polycard.cardEffects.neutral;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.RemoveBinomial;
import org.jspecify.annotations.Nullable;
import polycube.polycard.PolyCard;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.IsTargetedEventCallback;
import polycube.polycard.events.callBacks.ItemConsumedEventCallback;
import polycube.polycard.events.callBacks.ItemDurabilityChangeEventCallback;

import java.util.List;

public final class PiglinEffects extends CardEffects implements IsTargetedEventCallback, ItemDurabilityChangeEventCallback, ItemConsumedEventCallback {
    public static final int BUFF_DURATION = 20 * 15;
    public static final int BUFF_AMPLIFIER = 0;

    public static final List<Item> GOLD_FOOD = List.of(
            Items.GOLDEN_CARROT, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE
    );

    public static final List<Holder<MobEffect>> BUFFS = List.of(
            MobEffects.STRENGTH, MobEffects.HASTE, MobEffects.ABSORPTION,
            MobEffects.REGENERATION, MobEffects.LUCK, MobEffects.SPEED, MobEffects.FIRE_RESISTANCE,
            MobEffects.RESISTANCE, MobEffects.WATER_BREATHING, MobEffects.NIGHT_VISION
    );

    private static final RemoveBinomial PIGLIN_TOOLS_BINOMIAL = new RemoveBinomial(LevelBasedValue.constant(0.984F));
    private static final RemoveBinomial PIGLIN_ARMOR_BINOMIAL = new RemoveBinomial(LevelBasedValue.constant(0.8F));

    @Override
    public InteractionResult onTargeted(ServerLevel level, @Nullable LivingEntity targeter, LivingEntity target, IsTargetedEventCallback.TargetingConditionsData targetingConditionsData) {
        if (targetingConditionsData.isCombat() && target instanceof ServerPlayer player) {
            switch (targeter) {
                case Piglin _ -> {
                    if (hasCardOrRarer(player, RarityLevel.UNCOMMON)) {
                        return InteractionResult.FAIL;
                    }
                }
                case PiglinBrute _ -> {
                    if (hasCardOrRarer(player, RarityLevel.EPIC)) {
                        return InteractionResult.FAIL;
                    }
                }
                case null, default -> {
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public int onDurabilityChange(ServerLevel level, @Nullable ServerPlayer player, ItemStack itemStack, int amount) {
        if (player != null) {
            if (itemStack.is(ItemTags.PIGLIN_LOVED)) {
                if (hasCardOrRarer(player, RarityLevel.LEGENDARY)) {
                    if (itemStack.is(ItemTags.ARMOR_ENCHANTABLE)) {
                        return (int) PIGLIN_ARMOR_BINOMIAL.process(0, level.getRandom(), amount);
                    }
                    return (int) PIGLIN_TOOLS_BINOMIAL.process(0, level.getRandom(), amount);
                }
            }
        }
        return amount;
    }

    @Override
    public void onItemConsumed(ServerPlayer player, ItemStack item) {
        if (GOLD_FOOD.contains(item.getItem())) {
            if (hasCardOrRarer(player, RarityLevel.RARE)) {
                var effect = BUFFS.get(player.level().getRandom().nextInt(BUFFS.size()));
                PolyCard.LOGGER.debug("{} has the rare piglin card and consumed a gold food item. Applying random buff {}.", player.getName().getString(), effect.value().getDescriptionId());
                player.addEffect(new MobEffectInstance(effect, BUFF_DURATION, BUFF_AMPLIFIER));
            }
        }
    }
}
