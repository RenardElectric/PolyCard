package polycube.polycard.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.commands.commandArguments.CardTypeArgument;
import polycube.polycard.commands.commandArguments.RarityTypeArgument;
import polycube.polycard.manager.CardManager;

public class GiveCommand extends PolyCardCommand {

    public GiveCommand() {
        super(
                "give",
                "Give a card to a player",
                "<player> <cardType> [rarityLevel]",
                PermissionLevel.GAMEMASTERS
        );
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> getCommand() {
        return super.getCommand().then(
                Commands.argument("player", EntityArgument.players())
                        .then(
                                Commands.argument("cardType", StringArgumentType.string())
                                        .suggests(CardTypeArgument::suggestCards)
                                        .executes(cts -> giveCard(cts, false))
                                        .then(
                                                Commands.argument("rarityLevel", StringArgumentType.string())
                                                        .suggests(RarityTypeArgument::suggestRarities)
                                                        .executes(cts -> giveCard(cts, true))
                                        )
                        )
        );
    }

    private int giveCard(CommandContext<CommandSourceStack> cts, boolean withRarityLevel) throws CommandSyntaxException {
        var source = cts.getSource();
        var player = EntityArgument.getPlayer(cts, "player");
        var optionalCardType = CardTypeArgument.getType(cts, "cardType");

        if (optionalCardType.isEmpty()) {
            source.sendFailure(Component.literal("Invalid card type: " + StringArgumentType.getString(cts, "cardType")));
            return 0;
        }
        var cardType = optionalCardType.get();

        // Get rarity (default to minimum rarity level of the card)
        RarityLevel rarityLevel = cardType.minRarityLevel();
        if (withRarityLevel) {
            var optionalRarityType = RarityTypeArgument.getRarity(cts, "rarityLevel");

            if (optionalRarityType.isEmpty()) {
                source.sendFailure(Component.literal("Invalid rarity level: " + StringArgumentType.getString(cts, "rarityLevel")));
                return 0;
            }

            if (optionalRarityType.get().ordinal() < rarityLevel.ordinal()) {
                source.sendFailure(Component.literal(cardType + " requires at least " + rarityLevel + " rarity level."));
                return 0;
            }

            rarityLevel = optionalRarityType.get();
        }
        var card = new Card(cardType, rarityLevel);
        CardManager.giveCard(player, card);

        source.sendSuccess(() -> Component.literal(ChatFormatting.GREEN + "Gave " + player.getName().getString() + " a ").append(card.getFormattedName()), true);
        player.sendSystemMessage(Component.literal(ChatFormatting.GOLD + "You received a ").append(card.getFormattedName()));

        PolyCard.LOGGER.debug("[Polycard] Admin {} gave {} a {}", source.getDisplayName(), player.getName(), card);

        return 1;
    }
}
