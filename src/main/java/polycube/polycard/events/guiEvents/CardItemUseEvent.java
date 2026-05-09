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

public class CardItemUseEvent implements ItemUseEventCallback {
    private final CardManager cardManager;

    public CardItemUseEvent(CardManager cardManager) {
        this.cardManager = cardManager;
    }

    public InteractionResult interact(ServerPlayer player, Level world, InteractionHand hand) {
        // Only allow shift-click on cards
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        var item = player.getItemInHand(hand);

        if (item.isEmpty()) {
            return InteractionResult.PASS;
        }

        var optionalCard = CardManager.getCard(item);
        if (optionalCard.isEmpty()) {
            return InteractionResult.PASS;
        }
        var card = optionalCard.get();

        var storage = cardManager.getStorage();
        var optionalEquippedCard = storage.getEquippedCards(player).stream()
                .filter(c -> c.type() == card.type())
                .findFirst();

        // Swap the card if the player already has a card of the same type equipped
        if (optionalEquippedCard.isPresent()) {
            var equippedCard = optionalEquippedCard.get();

            if (equippedCard.rarity() == card.rarity()) {
                player.sendSystemMessage(Component.literal("You already equipped this card!").withStyle(ChatFormatting.RED));
                PolyCard.debug("{} tried to equip a card they already have equipped: {}", player.getName().getString(), card);
                return InteractionResult.PASS;
            }

            // Swap the cards
            storage.unequipCard(player, equippedCard);
            var result = equipCrad(storage, item, player, card);
            player.addItem(equippedCard.asItem());
            return result;
        }

        // Check if player already has 5 cards equipped
        if (storage.getEquippedCards(player).size() >= Storage.MAX_EQUIPPED_CARDS) {
            player.sendSystemMessage(Component.literal("You already have " + Storage.MAX_EQUIPPED_CARDS + " cards equipped!").withStyle(ChatFormatting.RED));
            PolyCard.debug("{} tried to equip a card but already has {} cards equipped: {}", player.getName().getString(), Storage.MAX_EQUIPPED_CARDS, card);
            return InteractionResult.PASS;
        }

        // Equip the card
        return equipCrad(storage, item, player, card);
    }

    private InteractionResult equipCrad(Storage storage, ItemStack item, ServerPlayer player, Card card) {
        if (storage.equipCard(player, card)) {
            item.setCount(item.getCount() - 1);
            player.sendSystemMessage(Component.literal(ChatFormatting.GREEN + "Equipped: ").append(card.getFormatedName()));
            PolyCard.debug("{} equipped card: {}", player.getName().getString(), card);
            cardManager.save();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
