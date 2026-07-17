package polycube.polycard.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.commands.commandArguments.CardTypeArgument;
import polycube.polycard.utils.CardHelper;
import polycube.polycard.utils.Helpers;

import java.util.EnumMap;

public class TestCommand extends PolyCardCommand {
    private static final int DEFAULT_ROLL_COUNT = 1_000;
    private static final int MAX_ROLL_COUNT = 10_000;

    public TestCommand() {
        super(
                "test",
                "Test the card rolling system",
                "<cardType> [cardsNumber]",
                PermissionLevel.GAMEMASTERS
        );
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> getCommand() {
        return super.getCommand().then(
                Commands.argument(CardTypeArgument.NAME, StringArgumentType.word())
                        .suggests(CardTypeArgument::suggestCards)
                        .executes(context -> execute(context, DEFAULT_ROLL_COUNT))
                        .then(
                                Commands.argument("cardsNumber", IntegerArgumentType.integer(1, MAX_ROLL_COUNT))
                                        .executes(context -> execute(context, IntegerArgumentType.getInteger(context, "cardsNumber")))
                        )
        );
    }

    private int execute(CommandContext<CommandSourceStack> context, int cardsNumber) {
        var optionalCardType = CardTypeArgument.getType(context);
        if (optionalCardType.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Invalid card type: " + StringArgumentType.getString(context, CardTypeArgument.NAME)));
            return 0;
        }

        var cardType = optionalCardType.get();
        int none = 0;
        var counts = new EnumMap<RarityLevel, Integer>(RarityLevel.class);
        for (int i = 0; i < cardsNumber; i++) {
            var rarity = CardHelper.getRandomRarityLevel(cardType);
            if (rarity.isPresent()) {
                counts.merge(rarity.get(), 1, Integer::sum);
            } else {
                none++;
            }
        }
        var message = Component.literal("Rolls: none=" + none);
        for (var rarityLevel : RarityLevel.values()) {
            message.append(", " + rarityLevel.getSerializedName() + "=" + counts.getOrDefault(rarityLevel, 0));
        }
        context.getSource().sendSuccess(() -> message, false);
        Helpers.debug("{} tested {} rolls for {}", context.getSource().getDisplayName(), cardsNumber, cardType);

        return 1;
    }
}
