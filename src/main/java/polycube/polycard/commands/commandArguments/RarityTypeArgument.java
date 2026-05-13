package polycube.polycard.commands.commandArguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import polycube.polycard.card.RarityLevel;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class RarityTypeArgument {
    private static final RarityLevel[] VALUES = RarityLevel.values();

    public static Optional<RarityLevel> getRarity(final CommandContext<CommandSourceStack> context, final String name) {
        String id = context.getArgument(name, String.class);
        return RarityLevel.deserialize(id);
    }

    public static <S> CompletableFuture<Suggestions> suggestRarities(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Arrays.stream(VALUES).map(RarityLevel::getSerializedName), builder);
    }
}
