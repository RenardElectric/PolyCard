package polycube.polycard.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.PolyCard;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.commands.commandArguments.CardGroupArgument;
import polycube.polycard.commands.commandArguments.CardTypeArgument;
import polycube.polycard.utils.CardHelpers;
import polycube.polycard.utils.Helpers;
import polycube.polycore.commands.PolyCommand;
import polycube.polycore.text.TextComponents;

import java.util.EnumMap;

public class TestCommand extends PolyCommand {
    private static final int DEFAULT_ROLL_COUNT = 1_000;
    private static final int MAX_ROLL_COUNT = 10_000;

    public TestCommand() {
        super(
                PolyCard.MOD_ID,
                "test",
                "Simulate card rarity rolls",
                "<cardGroup> <cardType> [cardsNumber]",
                PermissionLevel.GAMEMASTERS
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommand(String name) {
        return super.getCommand(name).then(
                Commands.argument(CardGroupArgument.NAME, StringArgumentType.word())
                        .suggests(CardGroupArgument::suggestGroups)
                        .then(
                                Commands.argument(CardTypeArgument.NAME, StringArgumentType.word())
                                        .suggests(CardTypeArgument::suggestCards)
                                        .executes(context -> execute(context, DEFAULT_ROLL_COUNT))
                                        .then(
                                                Commands.argument("cardsNumber", IntegerArgumentType.integer(1, MAX_ROLL_COUNT))
                                                        .executes(context -> execute(context, IntegerArgumentType.getInteger(context, "cardsNumber")))
                                        )
                        )
        );
    }

    private int execute(CommandContext<CommandSourceStack> context, int cardsNumber) {
        if (CardGroupArgument.getType(context).isEmpty()) {
            context.getSource().sendFailure(TextComponents.error("Invalid card group: " + StringArgumentType.getString(context, CardGroupArgument.NAME)));
            return 0;
        }

        var optionalCardType = CardTypeArgument.getType(context);
        if (optionalCardType.isEmpty()) {
            context.getSource().sendFailure(TextComponents.error("Invalid card type: " + StringArgumentType.getString(context, CardTypeArgument.NAME)));
            return 0;
        }

        var cardType = optionalCardType.get();
        int none = 0;
        var counts = new EnumMap<RarityLevel, Integer>(RarityLevel.class);
        for (int i = 0; i < cardsNumber; i++) {
            var rarity = CardHelpers.getRandomRarityLevel(cardType, null);
            if (rarity.isPresent()) {
                counts.merge(rarity.get(), 1, Integer::sum);
            } else {
                none++;
            }
        }
        var message = TextComponents.header("Card Rolling Test")
                .append(TextComponents.field("Card", TextComponents.value(cardType.toString())))
                .append(TextComponents.field("Rolls", TextComponents.value(cardsNumber)))
                .append(TextComponents.field("Results", Component.empty()))
                .append(TextComponents.indentedField("None", formatResult(none, cardsNumber)));
        for (var rarity : cardType.getRarities()) {
            var rarityLevel = rarity.rarityLevel();
            message.append(TextComponents.indentedField(
                    rarityLevel.toString(),
                    formatResult(counts.getOrDefault(rarityLevel, 0), cardsNumber)
            ));
        }
        context.getSource().sendSuccess(() -> message, false);
        PolyCard.LOGGER.debug("{} tested {} rolls for {}", context.getSource().getDisplayName(), cardsNumber, cardType);

        return 1;
    }

    private Component formatResult(int count, int total) {
        return TextComponents.value(count)
                .append(TextComponents.muted(" (" + Helpers.probToStr((double) count / total) + "%)"));
    }
}
