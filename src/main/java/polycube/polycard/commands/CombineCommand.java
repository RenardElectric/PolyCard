package polycube.polycard.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.utils.CardHelpers;

public class CombineCommand extends PolyCardCommand {

    public CombineCommand() {
        super(
                "combine",
                "Combine " + Card.CARDS_FOR_NEXT_LEVEL + " held cards into one card of the next rarity",
                "",
                PermissionLevel.ALL
        );
    }

    @Override
    protected int execute(CommandSourceStack source) {
        var player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("This command can only be executed by a player."));
            return 0;
        }

        var mainItemStack = player.getMainHandItem();
        var card = Card.getCard(mainItemStack);
        if (mainItemStack.isEmpty() || card.isEmpty() || mainItemStack.getCount() < Card.CARDS_FOR_NEXT_LEVEL) {
            source.sendFailure(Component.literal("You must be holding at least " + Card.CARDS_FOR_NEXT_LEVEL + " cards in your main hand to combine them."));
            return 0;
        }

        var nextCard = card.get().next();
        if (nextCard.isEmpty()) {
            source.sendFailure(Component.literal("The cards you are trying to combine are already at the highest rarity level."));
            return 0;
        }

        mainItemStack.shrink(Card.CARDS_FOR_NEXT_LEVEL);
        CardHelpers.giveCard(player, nextCard.get());
        source.sendSuccess(() -> Component.literal("Combined " + Card.CARDS_FOR_NEXT_LEVEL + " cards into ")
                .append(nextCard.get().getFormattedName()), false);
        PolyCard.LOGGER.debug("{} combined {} into {}", player.getName().getString(), card.get(), nextCard.get());
        return 1;
    }
}

