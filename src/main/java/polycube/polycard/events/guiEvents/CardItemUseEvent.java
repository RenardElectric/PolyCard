package polycube.polycard.events.guiEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.events.callBacks.ItemUseEventCallback;
import polycube.polycard.manager.CardManager;
import polycube.polycard.manager.Storage;

/// Handles equipping cards when a player uses a card item.
/// Prevents equipping if the player is sneaking,
/// already has a card of the same type equipped,
/// or has 5 cards equipped.
/// Swaps cards if the player equips a card of the same type but different rarity.
public class CardItemUseEvent implements ItemUseEventCallback {
    private final CardManager cardManager;

    public CardItemUseEvent(CardManager cardManager) {
        this.cardManager = cardManager;
    }

    public InteractionResult interact(ServerPlayer player, Level world, InteractionHand hand) {
        // Prevent equipping cards while sneaking
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

        var playerData = cardManager.getStorage().data(player);
        var optionalEquippedCard = playerData.getEquippedCards().stream()
                .filter(c -> c.cardType() == card.cardType())
                .findFirst();

        // Swap the card if the player already has a card of the same type equipped
        if (optionalEquippedCard.isPresent()) {
            var equippedCard = optionalEquippedCard.get();

            if (equippedCard.rarityType() == card.rarityType()) {
                player.sendSystemMessage(Component.literal("You already equipped this card!").withStyle(ChatFormatting.RED));
                PolyCard.debug("{} tried to equip a card they already have equipped: {}", player.getName().getString(), card);
                return InteractionResult.FAIL;
            }

            // Swap the cards
            playerData.unequipCard(equippedCard);
            var result = equipCrad(playerData, item, player, card);
            player.addItem(equippedCard.asItem());
            return result;
        }

        // Check if player already has 5 cards equipped
        if (playerData.getEquippedCards().size() >= Storage.MAX_EQUIPPED_CARDS) {
            player.sendSystemMessage(Component.literal("You already have " + Storage.MAX_EQUIPPED_CARDS + " cards equipped!").withStyle(ChatFormatting.RED));
            PolyCard.debug("{} tried to equip a card but already has {} cards equipped: {}", player.getName().getString(), Storage.MAX_EQUIPPED_CARDS, card);
            return InteractionResult.FAIL;
        }

        // Equip the card
        return equipCrad(playerData, item, player, card);
    }

    private InteractionResult equipCrad(Storage.PlayerData playerData, ItemStack item, ServerPlayer player, Card card) {
        if (playerData.equipCard(card)) {
            item.setCount(item.getCount() - 1);
            player.sendSystemMessage(Component.literal(ChatFormatting.GREEN + "Equipped: ").append(card.getFormatedName()));
            PolyCard.debug("{} equipped card: {}", player.getName().getString(), card);
            cardManager.save();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }
}
