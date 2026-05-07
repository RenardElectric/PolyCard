package polycube.polycard.commands.commandArguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import polycube.polycard.card.CardType;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public final class CardArgument {
    private static final CardType[] VALUES = CardType.values();

    public static <S> CompletableFuture<Suggestions> suggestCards(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return context.getSource() instanceof SharedSuggestionProvider
                ? SharedSuggestionProvider.suggest(Arrays.stream(VALUES).map(CardType::getId), builder)
                : Suggestions.empty();
    }
}
