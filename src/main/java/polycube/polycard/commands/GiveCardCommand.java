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
import polycube.polycard.card.Rarity;
import polycube.polycard.commands.commandArguments.CardArgument;
import polycube.polycard.commands.commandArguments.RarityArgument;
import polycube.polycard.manager.CardManager;

public class GiveCardCommand extends PolyCardCommand {

    private final CardManager cardManager;

    public GiveCardCommand(CardManager cardManager) {
        super(
                "givecard",
                "Give a card to a player (admin only).",
                "/" + PolyCard.MOD_ID + "givecard <player> <card> [rarity]",
                PermissionLevel.ADMINS
        );
        this.cardManager = cardManager;
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
        var optionalCard = Card.fromId(cardId);

        if (optionalCard.isEmpty()) {
            source.sendFailure(Component.literal("Card '" + cardId + "' not found"));
            return 0;
        }
        var card = optionalCard.get();

        // Get rarity (default to minimum rarity of the card)
        Rarity cardRarity = card.getMinRarity();
        if (withRarity) {
            var rarityId = cts.getArgument("rarity", String.class);
            var rarity = Rarity.fromId(rarityId);

            if (rarity.isEmpty()) {
                source.sendFailure(Component.literal("Rarity '" + rarityId + "' not found"));
                return 0;
            }

            if (rarity.get().ordinal() < cardRarity.ordinal()) {
                source.sendFailure(Component.literal(card.getName() + " requires at least " + cardRarity.getName() + " rarity."));
                return 0;
            }

            cardRarity = rarity.get();
        }

        ItemStack cardItem = cardManager.createCardItem(card, cardRarity);
        player.getInventory().add(cardItem);

        String rarityDisplayName = cardRarity.getColor() + cardRarity.getName() + ChatFormatting.RESET;
        cts.getSource().sendSuccess(() -> Component.literal(ChatFormatting.GREEN + "Gave " + player.getName().getString() + " a " + rarityDisplayName + ChatFormatting.GREEN + " " + card.getName() + "."), true);
        player.sendSystemMessage(Component.literal(ChatFormatting.GOLD + "You received a " + rarityDisplayName + ChatFormatting.GOLD + " " + card.getName() + "!"));

        PolyCard.LOGGER.info("[Polycard] Admin {} gave {} a {} {}", cts.getSource().getDisplayName(), player.getName(), cardRarity.getName(), card.getName());

        return 1;
    }
}
