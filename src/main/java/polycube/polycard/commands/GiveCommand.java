package polycube.polycard.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.commands.commandArguments.CardGroupArgument;
import polycube.polycard.commands.commandArguments.CardTypeArgument;
import polycube.polycard.commands.commandArguments.RarityLevelArgument;
import polycube.polycard.utils.CardHelpers;

public class GiveCommand extends PolyCardCommand {

    public GiveCommand() {
        super(
                "give",
                "Give a supported card to one or more players",
                "<players> <cardGroup> <cardType> [rarityLevel]",
                PermissionLevel.GAMEMASTERS
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommand(String name) {
        return super.getCommand(name).then(
                Commands.argument("player", EntityArgument.players())
                        .then(
                                Commands.argument(CardGroupArgument.NAME, StringArgumentType.word())
                                        .suggests(CardGroupArgument::suggestGroups)
                                        .then(
                                                Commands.argument(CardTypeArgument.NAME, StringArgumentType.word())
                                                        .suggests(CardTypeArgument::suggestCards)
                                                        .executes(cts -> giveCard(cts, false))
                                                        .then(
                                                                Commands.argument(RarityLevelArgument.NAME, StringArgumentType.word())
                                                                        .suggests(RarityLevelArgument::suggestRarities)
                                                                        .executes(cts -> giveCard(cts, true))
                                                        )
                                        )
                        )
        );
    }

    private int giveCard(CommandContext<CommandSourceStack> cts, boolean withRarityLevel) throws CommandSyntaxException {
        var source = cts.getSource();
        var players = EntityArgument.getPlayers(cts, "player");

        if (CardGroupArgument.getType(cts).isEmpty()) {
            source.sendFailure(CommandText.error("Invalid card group: " + StringArgumentType.getString(cts, CardGroupArgument.NAME)));
            return 0;
        }

        var optionalCardType = CardTypeArgument.getType(cts);

        if (optionalCardType.isEmpty()) {
            source.sendFailure(CommandText.error("Invalid card type: " + StringArgumentType.getString(cts, CardTypeArgument.NAME)));
            return 0;
        }
        var cardType = optionalCardType.get();

        // No rarity argument means "give the first rarity this card type supports."
        RarityLevel rarityLevel = cardType.minRarityLevel();
        if (withRarityLevel) {
            var optionalRarityLevel = RarityLevelArgument.getRarity(cts);

            if (optionalRarityLevel.isEmpty()) {
                source.sendFailure(CommandText.error("Invalid rarity level: " + StringArgumentType.getString(cts, RarityLevelArgument.NAME)));
                return 0;
            }
            rarityLevel = optionalRarityLevel.get();
        }

        var optionalCard = Card.tryCreate(cardType, rarityLevel);
        if (optionalCard.isEmpty()) {
            source.sendFailure(CommandText.error(cardType + " does not support " + rarityLevel + " rarity."));
            return 0;
        }
        var card = optionalCard.get();

        for (var player : players) {
            CardHelpers.giveCard(player, card);
            source.sendSuccess(() -> CommandText.success("Gave " + player.getName().getString() + " a ")
                    .append(card.getFormattedName()), true);
            player.sendSystemMessage(CommandText.success("You received a ").append(card.getFormattedName()));
            PolyCard.LOGGER.debug("Admin {} gave {} a {}", source.getDisplayName(), player.getName(), card);
        }

        return players.size();
    }
}
