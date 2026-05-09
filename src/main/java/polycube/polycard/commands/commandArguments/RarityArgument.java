package polycube.polycard.commands.commandArguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import polycube.polycard.card.Rarity;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class RarityArgument {
    private static final Rarity[] VALUES = Arrays.stream(Rarity.values()).sorted(Comparator.comparing(Rarity::ordinal)).toArray(Rarity[]::new);

    public static Optional<Rarity> getRarity(final CommandContext<CommandSourceStack> context, final String name) {
        String id = context.getArgument(name, String.class);
        return Rarity.deserialize(id);
    }

    public static <S> CompletableFuture<Suggestions> suggestRarities(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return context.getSource() instanceof SharedSuggestionProvider
                ? SharedSuggestionProvider.suggest(Arrays.stream(VALUES).map(Rarity::getSerializedName), builder)
                : Suggestions.empty();
    }
}
