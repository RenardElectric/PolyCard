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
import net.minecraft.world.item.ItemStack;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.Rarity;
import polycube.polycard.commands.commandArguments.CardArgument;
import polycube.polycard.commands.commandArguments.RarityArgument;
import polycube.polycard.manager.CardManager;

public class GiveCardCommand extends PolyCardCommand {

    public GiveCardCommand() {
        super(
                "givecard",
                "Give a card to a player (admin only).",
                "/" + PolyCard.MOD_ID + "givecard <player> <card> [rarity]",
                PermissionLevel.ADMINS
        );
    }

    @Override
    protected int execute(CommandSourceStack source) {
        source.sendFailure(Component.literal("Usage :" + getUsage()));
        return 0;
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> getCommand() {
        return super.getCommand().then(
                Commands.argument("player", EntityArgument.players())
                        .then(
                                Commands.argument("card", StringArgumentType.string())
                                        .suggests(CardArgument::suggestCards)
                                        .executes(cts -> giveCard(cts, false))
                                        .then(
                                                Commands.argument("rarity", StringArgumentType.string())
                                                        .suggests(RarityArgument::suggestRarities)
                                                        .executes(cts -> giveCard(cts, true))
                                        )
                        )
        );
    }

    private int giveCard(CommandContext<CommandSourceStack> cts, boolean withRarity) throws CommandSyntaxException {
        var source = cts.getSource();
        var player = EntityArgument.getPlayer(cts, "player");
        var cardId = cts.getArgument("card", String.class);
        var optionalCardType = CardType.fromId(cardId);

        if (optionalCardType.isEmpty()) {
            source.sendFailure(Component.literal("Card '" + cardId + "' not found"));
            return 0;
        }
        var cardType = optionalCardType.get();

        // Get rarity (default to minimum rarity of the card)
        Rarity cardRarity = cardType.getMinRarity();
        if (withRarity) {
            var rarityId = cts.getArgument("rarity", String.class);
            var rarity = Rarity.fromId(rarityId);

            if (rarity.isEmpty()) {
                source.sendFailure(Component.literal("Rarity '" + rarityId + "' not found"));
                return 0;
            }

            if (rarity.get().ordinal() < cardRarity.ordinal()) {
                source.sendFailure(Component.literal(cardType.getName() + " requires at least " + cardRarity.getName() + " rarity."));
                return 0;
            }

            cardRarity = rarity.get();
        }
        var card = new Card(cardType, cardRarity);
        player.getInventory().add(card.getItem());

        source.sendSuccess(() -> Component.literal(ChatFormatting.GREEN + "Gave " + player.getName().getString() + " a ").append(card.getFormatedName()), true);
        player.sendSystemMessage(Component.literal(ChatFormatting.GOLD + "You received a ").append(card.getFormatedName()));

        PolyCard.LOGGER.debug("[Polycard] Admin {} gave {} a {}", source.getDisplayName(), player.getName(), card.getDisplayName());

        return 1;
    }
}
