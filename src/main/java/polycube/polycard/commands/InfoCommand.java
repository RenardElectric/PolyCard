package polycube.polycard.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.commands.commandArguments.CardTypeArgument;

public class InfoCommand extends PolyCardCommand {
    public InfoCommand() {
        super(
                "info",
                "Get information about a specific card",
                "<cardType>",
                PermissionLevel.ALL
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

    @Override
    protected int execute(CommandSourceStack source) {
        PolyCardCommands.printModInfo(source);
        return 1;
    }

    protected int execute(CommandContext<CommandSourceStack> context) {
        var optionalCardType = CardTypeArgument.getType(context, "cardType");
        if (optionalCardType.isPresent()) {
            var cardType = optionalCardType.get();
            var message = Component.literal("\n" + cardType + " card:");
            for (var rarity : cardType.getRarities()) {
                var rarityInfo = Component.literal(" - " + rarity.rarityLevel() + " (" + rarity.probability() + "%)")
                        .append(Component.literal(" : " + rarity.description()))
                        .withStyle(rarity.rarityLevel().color());
                message.append("\n").append(rarityInfo);
            }
            context.getSource().sendSuccess(() -> message, false);
        } else {
            context.getSource().sendFailure(Component.literal("Invalid card type: " + StringArgumentType.getString(context, "cardType")));
            return 0;
        }

        return 1;
    }
}
