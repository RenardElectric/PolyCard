package polycube.polycard.events.cardEvents.neutral.enderMan;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.Rarity;
import polycube.polycard.events.ItemUseEvents;
import polycube.polycard.manager.CardManager;

public class EnderManHandler {

    public static void registerEnderManCardEvents(CardManager cardManager) {
        ItemUseEvents.registerItemUseEvents(
                (player, world, hand) -> onEnderPearlUsed(cardManager, player, world, hand)
        );
    }

    private static InteractionResult onEnderPearlUsed(CardManager cardManager, ServerPlayer player, Level world, InteractionHand hand) {
        var itemStack = player.getItemInHand(hand);
        if (itemStack.getItem() == Items.ENDER_PEARL) {
            if (cardManager.getStorage().hasCardOrRarer(player, new Card(CardType.ENDERMAN, Rarity.RARE))) {
                PolyCard.runLater(1, _ -> {
                    var cooldowns = player.getCooldowns();
                    cooldowns.removeCooldown(cooldowns.getCooldownGroup(itemStack));
                });

            }
        }

        return InteractionResult.PASS;
    }
}
