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
import polycube.polycard.card.RarityType;
import polycube.polycard.commands.commandArguments.CardTypeArgument;
import polycube.polycard.commands.commandArguments.RarityTypeArgument;

public class GiveCardCommand extends PolyCardCommand {

    public GiveCardCommand() {
        super(
                "givecard",
                "Give a card to a player",
                "<player> <card> [rarity]",
                PermissionLevel.ADMINS
        );
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> getCommand() {
        return super.getCommand().then(
                Commands.argument("player", EntityArgument.players())
                        .then(
                                Commands.argument("card", StringArgumentType.string())
                                        .suggests(CardTypeArgument::suggestCards)
                                        .executes(cts -> giveCard(cts, false))
                                        .then(
                                                Commands.argument("rarity", StringArgumentType.string())
                                                        .suggests(RarityTypeArgument::suggestRarities)
                                                        .executes(cts -> giveCard(cts, true))
                                        )
                        )
        );
    }

    private int giveCard(CommandContext<CommandSourceStack> cts, boolean withRarity) throws CommandSyntaxException {
        var source = cts.getSource();
        var player = EntityArgument.getPlayer(cts, "player");
        var optionalCardType = CardTypeArgument.getType(cts, "card");

        if (optionalCardType.isEmpty()) {
            source.sendFailure(Component.literal("Invalid card type: " + StringArgumentType.getString(cts, "card")));
            return 0;
        }
        var cardType = optionalCardType.get();

        // Get rarity (default to minimum rarity of the card)
        RarityType rarityType = cardType.minRarity();
        if (withRarity) {
            var optionalRarityType = RarityTypeArgument.getRarity(cts, "rarity");

            if (optionalRarityType.isEmpty()) {
                source.sendFailure(Component.literal("Invalid rarity type: " + StringArgumentType.getString(cts, "rarity")));
                return 0;
            }

            if (optionalRarityType.get().ordinal() < rarityType.ordinal()) {
                source.sendFailure(Component.literal(cardType + " requires at least " + rarityType + " rarity."));
                return 0;
            }

            rarityType = optionalRarityType.get();
        }
        var card = new Card(cardType, rarityType);
        player.getInventory().add(card.asItem());

        source.sendSuccess(() -> Component.literal(ChatFormatting.GREEN + "Gave " + player.getName().getString() + " a ").append(card.getFormatedName()), true);
        player.sendSystemMessage(Component.literal(ChatFormatting.GOLD + "You received a ").append(card.getFormatedName()));

        PolyCard.LOGGER.debug("[Polycard] Admin {} gave {} a {}", source.getDisplayName(), player.getName(), card);

        return 1;
    }
}
