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
        return getRandomRarityLevel(card).flatMap(rarityLevel -> Card.tryCreate(card, rarityLevel));
    }

    /// Independently rolls each rarity and returns the last tier whose roll meets its threshold.
    public static Optional<RarityLevel> getRandomRarityLevel(CardType cardType) {
        return cardType.getRarityDistribution().select(ThreadLocalRandom.current());
    }
}
