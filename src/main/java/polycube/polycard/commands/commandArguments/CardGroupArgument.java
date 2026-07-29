package polycube.polycard.commands.commandArguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import polycube.polycard.card.CardGroup;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class CardGroupArgument {
    public static final String NAME = "cardGroup";

    private static final CardGroup[] VALUES = CardGroup.values();

    public static <S> Optional<CardGroup> getType(final CommandContext<S> context) {
        String id = context.getArgument(NAME, String.class);
        return CardGroup.deserialize(id);
    }

    @SuppressWarnings("unused")
    public static <S> CompletableFuture<Suggestions> suggestGroups(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Arrays.stream(VALUES).map(CardGroup::getSerializedName), builder);
    }
}
