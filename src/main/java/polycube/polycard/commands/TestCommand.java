package polycube.polycard.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.commands.commandArguments.CardTypeArgument;
import polycube.polycard.utils.CardHelper;

public class TestCommand extends PolyCardCommand {

    public TestCommand() {
        super(
                "test",
                "Test the card rolling system",
                "<cardType>",
                PermissionLevel.GAMEMASTERS
        );
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> getCommand() {
        return super.getCommand().then(
                Commands.argument("cardType", StringArgumentType.string())
                        .suggests(CardTypeArgument::suggestCards)
                        .executes(this::execute)
                        .then(
                                Commands.argument("cardNumber", IntegerArgumentType.integer(1, 10000))
                                        .executes(this::execute)
                        )
        );
    }

    protected int execute(CommandContext<CommandSourceStack> context) {
        var optionalCardType = CardTypeArgument.getType(context, "cardType");
        if (optionalCardType.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Invalid card type: " + StringArgumentType.getString(context, "cardType")));
            return 0;
        }

        var cardType = optionalCardType.get();
        var cardNumber = 1000;
        try {
            cardNumber = IntegerArgumentType.getInteger(context, "cardNumber");
        } catch (Exception e) {
            // Optional argument was omitted; keep the default sample size.
        }
        int none = 0;
        int common = 0;
        int uncommon = 0;
        int rare = 0;
        int epic = 0;
        int legendary = 0;
        for (int i = 0; i < cardNumber; i++) {
            var rarity = CardHelper.getRandomRarityLevel(cardType);
            if (rarity.isPresent()) {
                switch (rarity.get()) {
                    case COMMON -> common++;
                    case UNCOMMON -> uncommon++;
                    case RARE -> rare++;
                    case EPIC -> epic++;
                    case LEGENDARY -> legendary++;
                }
            } else {
                none++;
            }
        }
        var message = Component.literal("Roll: " + none + " / " + common + " / " + uncommon + " / " + rare + " / " + epic + " / " + legendary);
        context.getSource().sendSuccess(() -> message, false);

        return 1;
    }
}
