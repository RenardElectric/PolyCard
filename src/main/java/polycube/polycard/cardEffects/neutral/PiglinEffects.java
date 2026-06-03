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
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.IsTargetedEventCallback;
import polycube.polycard.events.callBacks.ItemConsumedEventCallback;
import polycube.polycard.events.callBacks.ItemDurabilityChangeEventCallback;
import polycube.polycard.utils.Helpers;

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

    public static void register() {
        IsTargetedEventCallback.EVENT.register(PiglinEffects::canBeTargeted);
        ItemDurabilityChangeEventCallback.EVENT.register(PiglinEffects::reduceDurability);
        ItemConsumedEventCallback.EVENT.register(PiglinEffects::eatGoldFood);
    }

    private static InteractionResult canBeTargeted(ServerLevel level, @Nullable LivingEntity targeter, LivingEntity target, IsTargetedEventCallback.TargetingConditionsData targetingConditionsData) {
        if (target instanceof ServerPlayer player) {
            var playerData = PolyCard.STORAGE.getPlayerData(player);
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

    private static int reduceDurability(ServerLevel level, @Nullable ServerPlayer player, ItemStack itemStack, int amount) {
        if (player != null) {
            if (goldItems.contains(itemStack.getItem())) {
                if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.LEGENDARY)) {
                    Helpers.debug("{} has the legendary piglin card and used a gold item. Reducing durability loss.", player.getName().getString());
                    if (itemStack.is(ItemTags.ARMOR_ENCHANTABLE)) {
                        return (int) piglinArmorBinomial.process(0, level.getRandom(), amount);
                    }
                    return (int) piglinToolsBinomial.process(0, level.getRandom(), amount);
                }
            }
        }
        return amount;
    }

    private static void eatGoldFood(ServerPlayer player, ItemStack item) {
        if (item != null && goldFood.contains(item.getItem())) {
            if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.RARE)) {
                //noinspection resource
                var effect = BUFFS.get(player.level().getRandom().nextInt(BUFFS.size()));
                Helpers.debug("{} has the rare piglin card and consumed a gold food item. Applying random buff {}.", player.getName().getString(), effect.value().getDescriptionId());
                player.addEffect(new MobEffectInstance(effect, BUFF_DURATION, BUFF_AMPLIFIER));
            }
        }
    }
}
