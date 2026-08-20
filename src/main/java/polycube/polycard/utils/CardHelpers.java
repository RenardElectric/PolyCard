package polycube.polycard.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/// Card creation, drop rolling, inventory delivery, and equipment-attribute helpers.
public final class CardHelpers {
    private CardHelpers() {}

    /// Rolls for a card of this type and gives it to the player if the roll succeeds.
    public static void receiveCard(ServerPlayer player, CardType cardType) {
        createCard(cardType).ifPresent(
                card -> {
                    giveCard(player, card);
                    PolyCard.LOGGER.debug("{} received a card: {}", player.getName(), card);
                    player.sendSystemMessage(
                            Component.literal("✨ You found a ")
                                    .append(card.getFormattedName())
                                    .append(" card!")
                                    .withStyle(ChatFormatting.GREEN)
                    );
                }
        );
    }

    /// Gives a concrete card item and plays pickup feedback.
    public static void giveCard(ServerPlayer player, Card card) {
        player.getInventory().placeItemBackInInventory(card.asItem());
        Helpers.playSound(player, SoundEvents.ITEM_PICKUP);
    }

    /// Creates a concrete card if any supported rarity roll succeeds.
    public static Optional<Card> createCard(CardType card) {
        return getRandomRarityLevel(card).map(rarityLevel -> new Card(card, rarityLevel));
    }

    /// Uses one roll against cumulative rarity thresholds and returns the highest matching tier.
    /// For example, thresholds of 20% Common and 7% Uncommon produce 13% Common and 7%+
    /// Uncommon; this keeps the displayed "this rarity or better" odds truthful.
    public static Optional<RarityLevel> getRandomRarityLevel(CardType cardType) {
        return selectRarity(cardType, ThreadLocalRandom.current().nextFloat());
    }

    /// Maps a supplied [0,1) roll to the highest cumulative rarity it satisfies.
    /// Keeping this deterministic core separate makes probability boundaries straightforward to test.
    public static Optional<RarityLevel> selectRarity(CardType cardType, float roll) {
        return cardType.getRarityDistribution().select(roll);
    }
}
