package polycube.polycard.utils;

import net.minecraft.server.level.ServerPlayer;
import polycube.polycard.PolyCard;
import polycube.polycard.card.CardType;

/// Snapshot helper for running effects unlocked by a player's equipped rarity.
public class CardRarityConditions {
    // Index 0 means no card; higher indexes unlock all lower rarity effects.
    private static final boolean[][] RARITY_MATRIX = {
            {false, false, false, false, false},
            {true, false, false, false, false},
            {true, true, false, false, false},
            {true, true, true, false, false},
            {true, true, true, true, false},
            {true, true, true, true, true}
    };

    private final boolean[] conditions;

    private CardRarityConditions(ServerPlayer player, CardType cardType) {
        var highestRarityIndex = 0;
        var storedRarityLevel = PolyCard.STORAGE.getPlayerData(player).equippedCards().get(cardType);
        if (storedRarityLevel != null) {
            highestRarityIndex = storedRarityLevel.rank() + 1;
        }
        conditions = RARITY_MATRIX[highestRarityIndex];
    }

    /// Captures the player's current rarity thresholds for this card type.
    public static CardRarityConditions of(ServerPlayer player, CardType cardType) {
        return new CardRarityConditions(player, cardType);
    }

    /// Runs when the player has this card type at common or higher.
    public CardRarityConditions hasCommon(Runnable runnable) {
        if (conditions[0]) runnable.run();
        return this;
    }

    /// Runs when the player has this card type at uncommon or higher.
    public CardRarityConditions hasUncommon(Runnable runnable) {
        if (conditions[1]) runnable.run();
        return this;
    }

    /// Runs when the player has this card type at rare or higher.
    public CardRarityConditions hasRare(Runnable runnable) {
        if (conditions[2]) runnable.run();
        return this;
    }

    /// Runs when the player has this card type at epic or higher.
    public CardRarityConditions hasEpic(Runnable runnable) {
        if (conditions[3]) runnable.run();
        return this;
    }

    /// Runs when the player has this card type at legendary.
    public CardRarityConditions hasLegendary(Runnable runnable) {
        if (conditions[4]) runnable.run();
        return this;
    }

    /// Runs one branch depending on whether common-or-higher is unlocked.
    public CardRarityConditions hasCommon(Runnable runnable, Runnable elseCaseRunnable) {
        if (conditions[0]) runnable.run();
        else elseCaseRunnable.run();
        return this;
    }

    /// Runs one branch depending on whether uncommon-or-higher is unlocked.
    public CardRarityConditions hasUncommon(Runnable runnable, Runnable elseCaseRunnable) {
        if (conditions[1]) runnable.run();
        else elseCaseRunnable.run();
        return this;
    }

    /// Runs one branch depending on whether rare-or-higher is unlocked.
    public CardRarityConditions hasRare(Runnable runnable, Runnable elseCaseRunnable) {
        if (conditions[2]) runnable.run();
        else elseCaseRunnable.run();
        return this;
    }

    /// Runs one branch depending on whether epic-or-higher is unlocked.
    public CardRarityConditions hasEpic(Runnable runnable, Runnable elseCaseRunnable) {
        if (conditions[3]) runnable.run();
        else elseCaseRunnable.run();
        return this;
    }

    /// Runs one branch depending on whether legendary is unlocked.
    public CardRarityConditions hasLegendary(Runnable runnable, Runnable elseCaseRunnable) {
        if (conditions[4]) runnable.run();
        else elseCaseRunnable.run();
        return this;
    }
}
