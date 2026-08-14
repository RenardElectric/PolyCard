package polycube.polycard.commands.commandArguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import polycube.polycard.card.CardType;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class CardTypeArgument {
    public static final String NAME = "cardType";

    private static final CardType[] VALUES = CardType.values();

    public static <S> Optional<CardType> getType(final CommandContext<S> context) {
        String id = context.getArgument(NAME, String.class);
        return CardType.deserialize(id);
    }

    public static <S> CompletableFuture<Suggestions> suggestCards(final CommandContext<S> context, final SuggestionsBuilder builder) {
        var group = CardGroupArgument.getType(context);
        return SharedSuggestionProvider.suggest(
                group.map(cardGroup -> Arrays.stream(VALUES).filter(cardType -> cardType.getGroup() == cardGroup))
                        .orElseGet(() -> Arrays.stream(VALUES))
                        .map(CardType::getSerializedName),
                builder
        );

    }
}
