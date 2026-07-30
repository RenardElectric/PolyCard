package polycube.polycard.utils;

import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import polycube.polycard.PolyCard;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;

/// Snapshot helper for running effects unlocked by a player's equipped rarity.
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class CardRarityConditions {
    private final @Nullable RarityLevel equippedRarity;

    private CardRarityConditions(ServerPlayer player, CardType cardType) {
        equippedRarity = PolyCard.storage().getPlayerData(player).equippedRarityLevel(cardType);
    }

    /// Captures the player's current rarity thresholds for this card type.
    public static CardRarityConditions of(ServerPlayer player, CardType cardType) {
        return new CardRarityConditions(player, cardType);
    }

    /// Runs when the player has this card type at common or higher.
    public CardRarityConditions hasCommon(Runnable runnable) {
        if (hasAtLeast(RarityLevel.COMMON)) runnable.run();
        return this;
    }

    /// Runs when the player has this card type at uncommon or higher.
    public CardRarityConditions hasUncommon(Runnable runnable) {
        if (hasAtLeast(RarityLevel.UNCOMMON)) runnable.run();
        return this;
    }

    /// Runs when the player has this card type at rare or higher.
    public CardRarityConditions hasRare(Runnable runnable) {
        if (hasAtLeast(RarityLevel.RARE)) runnable.run();
        return this;
    }

    /// Runs when the player has this card type at epic or higher.
    public CardRarityConditions hasEpic(Runnable runnable) {
        if (hasAtLeast(RarityLevel.EPIC)) runnable.run();
        return this;
    }

    /// Runs when the player has this card type at legendary.
    public CardRarityConditions hasLegendary(Runnable runnable) {
        if (hasAtLeast(RarityLevel.LEGENDARY)) runnable.run();
        return this;
    }

    /// Runs one branch depending on whether common-or-higher is unlocked.
    public CardRarityConditions hasCommon(Runnable runnable, Runnable elseCaseRunnable) {
        if (hasAtLeast(RarityLevel.COMMON)) runnable.run();
        else elseCaseRunnable.run();
        return this;
    }

    /// Runs one branch depending on whether uncommon-or-higher is unlocked.
    public CardRarityConditions hasUncommon(Runnable runnable, Runnable elseCaseRunnable) {
        if (hasAtLeast(RarityLevel.UNCOMMON)) runnable.run();
        else elseCaseRunnable.run();
        return this;
    }

    /// Runs one branch depending on whether rare-or-higher is unlocked.
    public CardRarityConditions hasRare(Runnable runnable, Runnable elseCaseRunnable) {
        if (hasAtLeast(RarityLevel.RARE)) runnable.run();
        else elseCaseRunnable.run();
        return this;
    }

    /// Runs one branch depending on whether epic-or-higher is unlocked.
    public CardRarityConditions hasEpic(Runnable runnable, Runnable elseCaseRunnable) {
        if (hasAtLeast(RarityLevel.EPIC)) runnable.run();
        else elseCaseRunnable.run();
        return this;
    }

    /// Runs one branch depending on whether legendary is unlocked.
    public CardRarityConditions hasLegendary(Runnable runnable, Runnable elseCaseRunnable) {
        if (hasAtLeast(RarityLevel.LEGENDARY)) runnable.run();
        else elseCaseRunnable.run();
        return this;
    }

    private boolean hasAtLeast(RarityLevel rarity) {
        return equippedRarity != null && equippedRarity.isAtLeast(rarity);
    }
}
