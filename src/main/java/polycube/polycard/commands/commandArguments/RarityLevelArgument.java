package polycube.polycard.commands.commandArguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import polycube.polycard.card.Rarity;
import polycube.polycard.card.RarityLevel;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class RarityLevelArgument {
    public static final String NAME = "rarityLevel";

    private static final RarityLevel[] VALUES = RarityLevel.values();

    public static <S> Optional<RarityLevel> getRarity(final CommandContext<S> context) {
        String id = context.getArgument(NAME, String.class);
        return RarityLevel.deserialize(id);
    }

    public static <S> CompletableFuture<Suggestions> suggestRarities(final CommandContext<S> context, final SuggestionsBuilder builder) {
        var card = CardTypeArgument.getType(context);
        return SharedSuggestionProvider.suggest(
                card.map(cardType -> cardType.getRarities().stream().map(Rarity::rarityLevel))
                        .orElseGet(() -> Arrays.stream(VALUES))
                        .map(RarityLevel::getSerializedName),
                builder
        );
    }
}
