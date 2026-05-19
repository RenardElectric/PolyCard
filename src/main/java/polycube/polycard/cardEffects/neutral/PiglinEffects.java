package polycube.polycard.cardEffects.neutral;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.events.callBacks.IsTargetedEventCallback;
import polycube.polycard.events.callBacks.ItemDurabilityChangeEventCallback;
import polycube.polycard.manager.CardManager;
import polycube.polycard.mixin.TargetingConditionsMixin;

import java.util.List;

public class PiglinEffects {
    public static final CardType CARD_TYPE = CardType.PIGLIN;

    public static final List<Item> goldItems = List.of(
            Items.GOLDEN_SWORD, Items.GOLDEN_SHOVEL, Items.GOLDEN_PICKAXE,
            Items.GOLDEN_AXE, Items.GOLDEN_HOE, Items.GOLDEN_SPEAR,
            Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS,
            Items.GOLDEN_BOOTS, Items.GOLDEN_HORSE_ARMOR, Items.GOLDEN_NAUTILUS_ARMOR
    );

    public static void register(CardManager cardManager) {
        IsTargetedEventCallback.EVENT.register(
                (level, targeter, target, targetingConditionsData)
                        -> canBeTargeted(cardManager, level, targeter, target, targetingConditionsData)
        );

        ItemDurabilityChangeEventCallback.EVENT.register(
                (level, player, itemStack, amount)
                        -> reduceDurability(cardManager, level, player, itemStack, amount)
        );
    }

    private static InteractionResult canBeTargeted(CardManager cardManger, ServerLevel level, @Nullable LivingEntity targeter, LivingEntity target, IsTargetedEventCallback.TargetingConditionsData targetingConditionsData) {
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

    private static int reduceDurability(CardManager cardManger, ServerLevel level, @Nullable ServerPlayer player, ItemStack itemStack, int amount) {
        if (player != null) {
            if (goldItems.contains(itemStack.getItem())) {
                var playerData = cardManger.getStorage().data(player);
                if (playerData.hasCardOrRarer(CARD_TYPE, RarityLevel.RARE)) {
                    return amount; // TODO: decrease damage
                }
            }
        }
        return amount;
    }
}
