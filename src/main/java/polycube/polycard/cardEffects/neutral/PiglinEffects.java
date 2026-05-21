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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.RemoveBinomial;
import org.jspecify.annotations.Nullable;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.events.callBacks.IsTargetedEventCallback;
import polycube.polycard.events.callBacks.ItemConsumedEventCallback;
import polycube.polycard.events.callBacks.ItemDurabilityChangeEventCallback;
import polycube.polycard.manager.CardManager;

import java.util.List;

public class PiglinEffects {
    public static final CardType CARD_TYPE = CardType.PIGLIN;

    public static final int BUFF_DURATION = 20 * 15;
    public static final int BUFF_AMPLIFIER = 0;

    public static final List<Item> goldItems = List.of(
            Items.GOLDEN_SWORD, Items.GOLDEN_SHOVEL, Items.GOLDEN_PICKAXE,
            Items.GOLDEN_AXE, Items.GOLDEN_HOE, Items.GOLDEN_SPEAR,
            Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS,
            Items.GOLDEN_BOOTS, Items.GOLDEN_HORSE_ARMOR, Items.GOLDEN_NAUTILUS_ARMOR
    );

    public static final List<Item> goldFood = List.of(
            Items.GOLDEN_CARROT, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE
    );

    public static final List<Holder<MobEffect>> BUFFS = List.of(
            MobEffects.STRENGTH, MobEffects.HASTE, MobEffects.ABSORPTION,
            MobEffects.REGENERATION, MobEffects.LUCK, MobEffects.SPEED, MobEffects.FIRE_RESISTANCE, // TODO: I do not think that the luck effect even works
            MobEffects.RESISTANCE, MobEffects.WATER_BREATHING, MobEffects.NIGHT_VISION
    );

    private static final RemoveBinomial piglinToolsBinomial = new RemoveBinomial(LevelBasedValue.constant(0.984F));
    private static final RemoveBinomial piglinArmorBinomial = new RemoveBinomial(LevelBasedValue.constant(0.8F));

    public static void register(CardManager cardManager) {
        IsTargetedEventCallback.EVENT.register(
                (level, targeter, target, targetingConditionsData)
                        -> canBeTargeted(cardManager, level, targeter, target, targetingConditionsData)
        );

        ItemDurabilityChangeEventCallback.EVENT.register(
                (level, player, itemStack, amount)
                        -> reduceDurability(cardManager, level, player, itemStack, amount)
        );

        ItemConsumedEventCallback.EVENT.register(
                (player, item)
                        -> eatGoldFood(cardManager, player, item)
        );
    }

    private static InteractionResult canBeTargeted(CardManager cardManger, ServerLevel level, @Nullable LivingEntity targeter, LivingEntity target, IsTargetedEventCallback.TargetingConditionsData targetingConditionsData) {
        if (target instanceof Player player) {
            var playerData = cardManger.getStorage().data(player);
            switch (targeter) {
                case Piglin _ -> {
                    if (playerData.hasCardOrRarer(CARD_TYPE, RarityLevel.UNCOMMON)) {
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

    private static int reduceDurability(CardManager cardManger, ServerLevel level, @Nullable ServerPlayer player, ItemStack itemStack, int amount) {
        if (player != null) {
            if (goldItems.contains(itemStack.getItem())) {
                var playerData = cardManger.getStorage().data(player);
                if (playerData.hasCardOrRarer(CARD_TYPE, RarityLevel.LEGENDARY)) {
                    if (itemStack.is(ItemTags.ARMOR_ENCHANTABLE)) {
                        return (int) piglinArmorBinomial.process(0, level.getRandom(), amount);
                    }
                    return (int) piglinToolsBinomial.process(0, level.getRandom(), amount);
                }
            }
        }
        return amount;
    }

    private static void eatGoldFood(CardManager cardManager, ServerPlayer player, ItemStack item) {
        if (item != null && goldFood.contains(item.getItem())) {
            var playerData = cardManager.getStorage().data(player);
            if (playerData.hasCardOrRarer(CARD_TYPE, RarityLevel.RARE)) {
                //noinspection resource
                var effect = BUFFS.get(player.level().getRandom().nextInt(BUFFS.size()));
                player.addEffect(new MobEffectInstance(effect, BUFF_DURATION, BUFF_AMPLIFIER));
            }
        }
    }
}
