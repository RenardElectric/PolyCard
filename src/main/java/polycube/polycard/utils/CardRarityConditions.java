package polycube.polycard.utils;

import net.minecraft.server.level.ServerPlayer;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;

import java.util.List;
import java.util.Map;

/// A utility class for checking card rarity conditions for a player and executing code based on those conditions.
public class CardRarityConditions {
    private static final Map<Byte, List<Boolean>> conditionsCache = Map.of(
            (byte) 0, List.of(false, false, false, false, false),
            (byte) 1, List.of(true, false, false, false, false),
            (byte) 2, List.of(true, true, false, false, false),
            (byte) 3, List.of(true, true, true, false, false),
            (byte) 4, List.of(true, true, true, true, false),
            (byte) 5, List.of(true, true, true, true, true)
    );

    private final List<Boolean> conditions;

    private CardRarityConditions(ServerPlayer player, CardType cardType) {
        var highestRarityIndex = PolyCard.STORAGE.getPlayerData(player).getEquippedCards().stream()
                .filter(card -> card.cardType() == cardType)
                .map(Card::rarityLevel)
                .map(RarityLevel::ordinal)
                .findAny().orElse(-1) + 1;

        conditions = conditionsCache.get((byte) highestRarityIndex);
    }

    /// Create a new CardRarityConditions instance for the given player and card type.
    ///
    /// @param player   The player to check the conditions for.
    /// @param cardType The type of card to check the conditions for.
    /// @return A new CardRarityConditions instance for the given player and card type.
    public static CardRarityConditions of(ServerPlayer player, CardType cardType) {
        return new CardRarityConditions(player, cardType);
    }

    /// Check if the player has the common card and execute the given runnable if they do.
    ///
    /// @param runnable The code to execute if the player has the common card.
    /// @return This CardRarityConditions instance for chaining.
    public CardRarityConditions hasCommon(Runnable runnable) {
        if (conditions.getFirst()) runnable.run();
        return this;
    }

    /// Check if the player has the uncommon card and execute the given runnable if they do.
    ///
    /// @param runnable The code to execute if the player has the uncommon card.
    /// @return This CardRarityConditions instance for chaining.
    public CardRarityConditions hasUncommon(Runnable runnable) {
        if (conditions.get(1)) runnable.run();
        return this;
    }

    /// Check if the player has the rare card and execute the given runnable if they do.
    ///
    /// @param runnable The code to execute if the player has the rare card.
    /// @return This CardRarityConditions instance for chaining.
    public CardRarityConditions hasRare(Runnable runnable) {
        if (conditions.get(2)) runnable.run();
        return this;
    }

    /// Check if the player has the epic card and execute the given runnable if they do.
    ///
    /// @param runnable The code to execute if the player has the epic card.
    /// @return This CardRarityConditions instance for chaining.
    public CardRarityConditions hasEpic(Runnable runnable) {
        if (conditions.get(3)) runnable.run();
        return this;
    }

    /// Check if the player has the legendary card and execute the given runnable if they do.
    ///
    /// @param runnable The code to execute if the player has the legendary card.
    /// @return This CardRarityConditions instance for chaining.
    public CardRarityConditions hasLegendary(Runnable runnable) {
        if (conditions.get(4)) runnable.run();
        return this;
    }

    /// Check if the player has the common card and execute the given runnable if they do, or the elseCaseRunnable if they don't.
    ///
    /// @param runnable         The code to execute if the player has the common card.
    /// @param elseCaseRunnable The code to execute if the player does not have the common card.
    /// @return This CardRarityConditions instance for chaining.
    public CardRarityConditions hasCommon(Runnable runnable, Runnable elseCaseRunnable) {
        if (conditions.getFirst()) runnable.run();
        else elseCaseRunnable.run();
        return this;
    }

    /// Check if the player has the uncommon card and execute the given runnable if they do, or the elseCaseRunnable if they don't.
    ///
    /// @param runnable         The code to execute if the player has the uncommon card.
    /// @param elseCaseRunnable The code to execute if the player does not have the uncommon card.
    /// @return This CardRarityConditions instance for chaining.
    public CardRarityConditions hasUncommon(Runnable runnable, Runnable elseCaseRunnable) {
        if (conditions.get(1)) runnable.run();
        else elseCaseRunnable.run();
        return this;
    }

    /// Check if the player has the rare card and execute the given runnable if they do, or the elseCaseRunnable if they don't.
    ///
    /// @param runnable         The code to execute if the player has the rare card.
    /// @param elseCaseRunnable The code to execute if the player does not have the rare card.
    /// @return This CardRarityConditions instance for chaining.
    public CardRarityConditions hasRare(Runnable runnable, Runnable elseCaseRunnable) {
        if (conditions.get(2)) runnable.run();
        else elseCaseRunnable.run();
        return this;
    }

    /// Check if the player has the epic card and execute the given runnable if they do, or the elseCaseRunnable if they don't.
    ///
    /// @param runnable         The code to execute if the player has the epic card.
    /// @param elseCaseRunnable The code to execute if the player does not have the epic card.
    /// @return This CardRarityConditions instance for chaining.
    public CardRarityConditions hasEpic(Runnable runnable, Runnable elseCaseRunnable) {
        if (conditions.get(3)) runnable.run();
        else elseCaseRunnable.run();
        return this;
    }

    /// Check if the player has the legendary card and execute the given runnable if they do, or the elseCaseRunnable if they don't.
    ///
    /// @param runnable         The code to execute if the player has the legendary card.
    /// @param elseCaseRunnable The code to execute if the player does not have the legendary card.
    /// @return This CardRarityConditions instance for chaining.
    public CardRarityConditions hasLegendary(Runnable runnable, Runnable elseCaseRunnable) {
        if (conditions.get(4)) runnable.run();
        else elseCaseRunnable.run();
        return this;
    }
}
