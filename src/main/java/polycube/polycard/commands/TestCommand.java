package polycube.polycard.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.card.CardType;
import polycube.polycard.commands.commandArguments.CardTypeArgument;
import polycube.polycard.manager.CardManager;

public class TestCommand extends PolyCardCommand {

    public TestCommand() {
        super(
                "test",
                "Test the card rolling system",
                "",
                PermissionLevel.GAMEMASTERS
        );
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> getCommand() {
        return super.getCommand().then(
                Commands.argument("cardType", StringArgumentType.string())
                        .suggests(CardTypeArgument::suggestCards)
                        .executes(this::execute)
        );
    }

    protected int execute(CommandContext<CommandSourceStack> context) {
        var cardType = CardTypeArgument.getType(context, "cardType").orElse(CardType.COW);
        int none = 0;
        int common = 0;
        int uncommon = 0;
        int rare = 0;
        int epic = 0;
        int legendary = 0;
        for (int i = 0; i < 1000; i++) {
            var rarity = CardManager.getRandomRarityLevel(cardType);
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
//        source.sendSuccess(() -> Component.literal("Expected: 500 / 250 / 150 / 90 / 10"), false);

        return 1;
    }
}
