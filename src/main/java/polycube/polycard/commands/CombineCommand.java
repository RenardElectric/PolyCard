package polycube.polycard.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.card.Card;
import polycube.polycard.manager.CardManager;

public class CombineCommand extends PolyCardCommand {

    public CombineCommand() {
        super(
                "combine",
                "Combine the cards the player is holding into a new card of a greater rarity",
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

        var mainItemStack =  player.getMainHandItem();
        var optionalCard = Card.getCard(mainItemStack);
        if (mainItemStack.isEmpty() || optionalCard.isEmpty() || mainItemStack.getCount() < Card.CARDS_FOR_NEXT_LEVEL) {
            source.sendFailure(Component.literal("You must be holding at least " + Card.CARDS_FOR_NEXT_LEVEL + " cards in your main hand to combine them."));
            return 0;
        }

        var card = optionalCard.get();
        var nextRarityLevel = card.rarityLevel().nextLevel();
        if (nextRarityLevel.isEmpty()) {
            source.sendFailure(Component.literal("The cards you are trying to combine are already at the highest rarity level."));
            return 0;
        }

        mainItemStack.shrink(Card.CARDS_FOR_NEXT_LEVEL);
        var newCard = new Card(card.cardType(), nextRarityLevel.get());
        CardManager.giveCard(player, newCard);
        return 1;
    }
}

