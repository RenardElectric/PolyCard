package polycube.polycard.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.PolyCard;
import polycube.polycard.commands.commandArguments.CardGroupArgument;
import polycube.polycard.commands.commandArguments.CardTypeArgument;
import polycube.polycard.utils.Helpers;
import polycube.polycore.commands.PolyCommand;
import polycube.polycore.text.TextComponents;

public class InfoCommand extends PolyCommand {
    public InfoCommand() {
        super(
                PolyCard.MOD_ID,
                "info",
                "Show how to acquire a card and list its rarity chances and effects",
                "<cardGroup> <cardType>",
                PermissionLevel.ALL
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
                                        .executes(this::execute)
                        )
        );
    }

    protected int execute(CommandContext<CommandSourceStack> context) {
        if (CardGroupArgument.getType(context).isEmpty()) {
            context.getSource().sendFailure(TextComponents.error("Invalid card group: " + StringArgumentType.getString(context, CardGroupArgument.NAME)));
            return 0;
        }

        var optionalCardType = CardTypeArgument.getType(context);
        if (optionalCardType.isPresent()) {
            var cardType = optionalCardType.get();
            var message = TextComponents.header("Card Details")
                    .append(TextComponents.field("Group", TextComponents.value(cardType.getGroup())))
                    .append(TextComponents.field("Card", TextComponents.value(cardType)))
                    .append(TextComponents.field("Acquired by", TextComponents.value(cardType.getCondition())))
                    .append(TextComponents.field("Rarities", Component.empty()));
            for (var rarity : cardType.getRarities()) {
                var details = rarity.getFormattedDescription().copy().append(TextComponents.muted(" (" + Helpers.probToStr(rarity.probability()) + "% chance)"));
                message.append(TextComponents.indentedField(rarity.rarityLevel().toString(), details));
            }
            context.getSource().sendSuccess(() -> message, false);
        } else {
            context.getSource().sendFailure(TextComponents.error("Invalid card type: " + StringArgumentType.getString(context, CardTypeArgument.NAME)));
            return 0;
        }

        return 1;
    }
}
