package polycube.polycard.events;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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

        var optionalCard = Card.getCard(item);
        if (optionalCard.isEmpty()) {
            return InteractionResult.PASS;
        }

        var playerData = PolyCard.storage().getPlayerData(player);
        var card = optionalCard.get();
        var cardType = card.cardType();
        var mutexGroup = cardType.getMutexGroup();

        Card replacingCard = null;
        for (var equippedCard : playerData.getEquippedCards()) {
            var equippedCardType = equippedCard.cardType();
            if ((equippedCardType == cardType && equippedCard.rarityLevel() != card.rarityLevel())
                    || (!mutexGroup.isBlank() && equippedCardType.getMutexGroup().equals(mutexGroup))) {
                replacingCard = equippedCard;
                break;
            }
        }

        if (replacingCard != null) {
            if (PlayerData.unequipCard(player, replacingCard).isSuccess()) {
                var result = equipCard(item, player, card);
                if (result.equals(InteractionResult.SUCCESS)) {
                    player.getInventory().placeItemBackInInventory(replacingCard.asItem());
                } else {
                    PlayerData.equipCard(player, replacingCard);
                    Helpers.debug("Rolled back a failed card swap for {}", player.getName().getString());
                }
                return result;
            }
        }

        return equipCard(item, player, card);
    }

    private InteractionResult equipCard(ItemStack item, ServerPlayer player, Card card) {
        return PlayerData.equipCard(player, card).mapOrElse(
                _ -> {
                    item.shrink(1);
                    Helpers.SendSuccess(player, Component.literal("Equipped card: ").append(card.getFormattedName()));
                    Helpers.playSound(player, SoundEvents.BUNDLE_INSERT);
                    Helpers.debug("{} equipped card: {}", player.getName().getString(), card);
                    return InteractionResult.SUCCESS;
                },
                error -> {
                    var msg = error.message();
                    Helpers.SendFailure(player, Component.literal("Failed to equip card: " + msg));
                    Helpers.debug("Failed to equip card for {}: {}", player.getName().getString(), msg);
                    return InteractionResult.FAIL;
                }
        );
    }
}
