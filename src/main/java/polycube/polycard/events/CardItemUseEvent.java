package polycube.polycard.events;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.ItemUseEventCallback;
import polycube.polycard.utils.Helpers;

/// Equips a card from the player's hand or swaps it with an equipped card of the same type.
/// Sneaking leaves the item use untouched so players can still access normal item behavior.
public class CardItemUseEvent extends EventHandler implements ItemUseEventCallback {
    @Override
    public InteractionResult onItemUse(ServerPlayer player, Level world, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        var item = player.getItemInHand(hand);

        if (item.isEmpty()) {
            return InteractionResult.PASS;
        }

        var card = Card.getCard(item);
        return card.map(value -> equipCard(item, player, hand, value)).orElse(InteractionResult.PASS);
    }

    private InteractionResult equipCard(ItemStack item, ServerPlayer player, InteractionHand hand, Card card) {
        var replacedCard = PlayerData.replacementFor(player, card).filter(equippedCard -> !equippedCard.equals(card));
        if (item.getCount() > 1 && replacedCard.isPresent()) {
            var returnedStack = replacedCard.get().asItem();
            var inventory = player.getInventory();
            if (inventory.getFreeSlot() == -1 && inventory.getSlotWithRemainingSpace(returnedStack) == -1) {
                Helpers.SendFailure(player, Component.literal("Failed to equip card: make room for the replaced card."));
                return InteractionResult.FAIL;
            }
        }

        return PlayerData.equipOrReplaceCard(player, card).mapOrElse(
                change -> {
                    item.shrink(1);
                    if (item.isEmpty() && change.unequipped().size() == 1) {
                        player.setItemInHand(hand, change.unequipped().getFirst().asItem());
                    } else {
                        change.unequipped().forEach(unequippedCard -> player.getInventory().placeItemBackInInventory(unequippedCard.asItem(), Prediction.SERVER_ONLY));
                    }
                    Helpers.SendSuccess(player, Component.literal("Equipped card: ").append(card.getFormattedName()));
                    Helpers.playSound(player, SoundEvents.BUNDLE_INSERT);
                    PolyCard.LOGGER.debug("{} equipped card: {}", player.getName().getString(), card);
                    return InteractionResult.SUCCESS;
                },
                error -> {
                    var msg = error.message();
                    Helpers.SendFailure(player, Component.literal("Failed to equip card: " + msg));
                    PolyCard.LOGGER.debug("Failed to equip card for {}: {}", player.getName().getString(), msg);
                    return InteractionResult.FAIL;
                }
        );
    }
}
