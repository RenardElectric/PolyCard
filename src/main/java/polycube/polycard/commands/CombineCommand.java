package polycube.polycard.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.utils.CardHelpers;

public class CombineCommand extends PolyCardCommand {

    public CombineCommand() {
        super(
                "combine",
                "Combine " + Card.CARDS_FOR_NEXT_LEVEL + " matching cards held in your main hand into one card of the next rarity",
                "",
                PermissionLevel.ALL,
                true
        );
    }

    @Override
    protected int execute(CommandSourceStack source) {
        var player = source.getPlayer();

        if (player == null) {
            source.sendFailure(CommandText.error("This command can only be executed by a player."));
            return 0;
        }

        var mainItemStack = player.getMainHandItem();
        var card = Card.getCard(mainItemStack);
        if (mainItemStack.isEmpty() || card.isEmpty() || mainItemStack.getCount() < Card.CARDS_FOR_NEXT_LEVEL) {
            source.sendFailure(CommandText.error("You must be holding at least " + Card.CARDS_FOR_NEXT_LEVEL + " cards in your main hand to combine them."));
            return 0;
        }

        var nextCard = card.get().next();
        if (nextCard.isEmpty()) {
            source.sendFailure(CommandText.error("The cards you are trying to combine are already at the highest rarity level."));
            return 0;
        }

        var inventory = player.getInventory();
        if (mainItemStack.getCount() > Card.CARDS_FOR_NEXT_LEVEL
                && inventory.getFreeSlot() == -1
                && inventory.getSlotWithRemainingSpace(nextCard.get().asItem()) == -1) {
            source.sendFailure(CommandText.error("Make room in your inventory before combining these cards."));
            return 0;
        }

        mainItemStack.shrink(Card.CARDS_FOR_NEXT_LEVEL);
        CardHelpers.giveCard(player, nextCard.get());
        source.sendSuccess(() -> CommandText.success("Combined " + Card.CARDS_FOR_NEXT_LEVEL + " cards into ")
                .append(nextCard.get().getFormattedName()), false);
        PolyCard.LOGGER.debug("{} combined {} into {}", player.getName().getString(), card.get(), nextCard.get());
        return 1;
    }
}

