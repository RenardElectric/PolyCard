package polycube.polycard.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.commands.commandArguments.CardTypeArgument;
import polycube.polycard.utils.Helpers;

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
                Commands.argument(CardTypeArgument.NAME, StringArgumentType.word())
                        .suggests(CardTypeArgument::suggestCards)
                        .executes(this::execute)
        );
    }

    protected int execute(CommandContext<CommandSourceStack> context) {
        var optionalCardType = CardTypeArgument.getType(context);
        if (optionalCardType.isPresent()) {
            var cardType = optionalCardType.get();
            var message = Component.literal("\n" + cardType + " card");
            message.append(
                    Component.literal("\n(Acquired by " + cardType.getCondition() + ")")
                            .withStyle(ChatFormatting.DARK_GRAY)
            );
            for (var rarity : cardType.getRarities()) {
                var rarityInfo = Component.literal(" - " + rarity.rarityLevel() + " (" + Helpers.probToStr(rarity.probability()) + "% chance or better)")
                        .append(Component.literal(" : " + rarity.description()))
                        .withStyle(rarity.rarityLevel().color());
                message.append("\n").append(rarityInfo);
            }
            context.getSource().sendSuccess(() -> message, false);
        } else {
            context.getSource().sendFailure(Component.literal("Invalid card type: " + StringArgumentType.getString(context, CardTypeArgument.NAME)));
            return 0;
        }

        return 1;
    }
}
