package polycube.polycard.commands.commandArguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import polycube.polycard.card.CardType;
import polycube.polycard.card.Rarity;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class CardTypeArgument {
    private static final CardType[] VALUES = CardType.values();

    public static Optional<CardType> getType(final CommandContext<CommandSourceStack> context, final String name) {
        String id = context.getArgument(name, String.class);
        return CardType.deserialize(id);
    }

    public static <S> CompletableFuture<Suggestions> suggestCards(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Arrays.stream(VALUES).map(CardType::getSerializedName), builder);
    }
}
