package polycube.polycard.events.guiEvents;

import net.minecraft.ChatFormatting;
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
import polycube.polycard.events.callBacks.CardEventCallback;
import polycube.polycard.events.callBacks.ItemUseEventCallback;
import polycube.polycard.utils.Helpers;

/// Equips a card from the player's hand, or swaps it with an equipped card of the same type.
/// Sneaking leaves the item use untouched so players can still access normal item behavior.
public class CardItemUseEvent implements ItemUseEventCallback {
    public InteractionResult interact(ServerPlayer player, Level world, InteractionHand hand) {
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
        var card = optionalCard.get();

        var playerData = PolyCard.STORAGE.getPlayerData(player);
        var equippedRarityLevel = playerData.equippedCards().get(card.cardType());

        if (equippedRarityLevel != null) {
            if (equippedRarityLevel == card.rarityLevel()) {
                Helpers.playFailure(player);
                player.sendSystemMessage(Component.literal("You already equipped this card!").withStyle(ChatFormatting.RED));
                Helpers.debug("{} tried to equip a card they already have equipped: {}", player.getName().getString(), card);
                return InteractionResult.FAIL;
            }

            // Different rarity of the same type: equip the held card and return the old one.
            if (playerData.unequipCardType(card.cardType())) {
                var equippedCard = new Card(card.cardType(), equippedRarityLevel);
                CardEventCallback.UNEQUIPPED.invoker().cardEvent(player, equippedCard);
                var result = equipCard(playerData, item, player, card);
                player.getInventory().placeItemBackInInventory(equippedCard.asItem());
                return result;
            }
            return InteractionResult.FAIL;
        }

        if (playerData.equippedCards().size() >= PlayerData.MAX_EQUIPPED_CARDS) {
            Helpers.playFailure(player);
            player.sendSystemMessage(Component.literal("You already have " + PlayerData.MAX_EQUIPPED_CARDS + " cards equipped!").withStyle(ChatFormatting.RED));
            Helpers.debug("{} tried to equip a card but already has {} cards equipped: {}", player.getName().getString(), PlayerData.MAX_EQUIPPED_CARDS, card);
            return InteractionResult.FAIL;
        }

        return equipCard(playerData, item, player, card);
    }

    private InteractionResult equipCard(PlayerData playerData, ItemStack item, ServerPlayer player, Card card) {
        if (playerData.equipCard(card)) {
            CardEventCallback.EQUIPPED.invoker().cardEvent(player, card);
            item.shrink(1);
            player.sendSystemMessage(Component.literal(ChatFormatting.GREEN + "Equipped: ").append(card.getFormattedName()));
            Helpers.debug("{} equipped card: {}", player.getName().getString(), card);
            Helpers.playSound(player, SoundEvents.BUNDLE_INSERT);
            PolyCard.STORAGE.save();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }
}
