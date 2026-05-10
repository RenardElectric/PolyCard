package polycube.polycard.commands.commandArguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import polycube.polycard.card.RarityType;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class RarityTypeArgument {
    private static final RarityType[] VALUES = RarityType.values();

    public static Optional<RarityType> getRarity(final CommandContext<CommandSourceStack> context, final String name) {
        String id = context.getArgument(name, String.class);
        return RarityType.deserialize(id);
    }

    public static <S> CompletableFuture<Suggestions> suggestRarities(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Arrays.stream(VALUES).map(RarityType::getSerializedName), builder);
    }
}
