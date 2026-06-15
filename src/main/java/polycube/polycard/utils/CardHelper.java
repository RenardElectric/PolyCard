package polycube.polycard.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/// Card creation, drop rolling, inventory delivery, and equipment-attribute helpers.
public class CardHelper {
    /// Rolls for a card of this type and gives it to the player if the roll succeeds.
    public static void receiveCard(ServerPlayer player, CardType cardType) {
        createCard(cardType).ifPresent(
                card -> {
                    giveCard(player, card);
                    Helpers.debug("{} received a card: {}", player.getName(), card);
                    player.sendSystemMessage(
                            Component.literal("✨ You found a ")
                                    .append(card.getFormattedName())
                                    .append(" card!")
                                    .withStyle(ChatFormatting.GREEN)
                    );
                }
        );
    }

    /// Re-applies equipped-card attributes when a player object is created.
    public static void loadPlayerAttributes(ServerPlayer player) {
        for (var card : PolyCard.STORAGE.getPlayerData(player).getEquippedCards()) {
            addCardAttributes(player, card);
        }
    }

    /// Adds this card's transient attribute modifiers to the player.
    public static void addCardAttributes(ServerPlayer player, Card card) {
        player.getAttributes().addTransientAttributeModifiers(card.getAttributeModifiers());
    }

    /// Removes this card's transient attribute modifiers from the player.
    public static void removeCardAttributes(ServerPlayer player, Card card) {
        player.getAttributes().removeAttributeModifiers(card.getAttributeModifiers());
    }

    /// Gives a concrete card item and plays pickup feedback.
    public static void giveCard(ServerPlayer player, Card card) {
        player.getInventory().placeItemBackInInventory(card.asItem());
        Helpers.playSound(player, SoundEvents.ITEM_PICKUP);
    }

    /// Creates a concrete card if any supported rarity roll succeeds.
    public static Optional<Card> createCard(CardType card) {
        return getRandomRarityLevel(card).map(
                rarityLevel -> new Card(card, rarityLevel)
        );
    }

    /// Rolls rarities from highest to lowest; each configured probability is tested independently.
    public static Optional<RarityLevel> getRandomRarityLevel(CardType cardType) {
        var rarities = new ArrayList<>(cardType.getRarities());
        Collections.reverse(rarities);
        for (var rarity : rarities) {
            if (rollsUnder(rarity.probability())) {
                return Optional.of(rarity.rarityLevel());
            }
        }
        return Optional.empty();
    }

    private static boolean rollsUnder(float probability) {
        if (probability <= 0) {
            return false;
        }

        return ThreadLocalRandom.current().nextFloat() < Math.min(probability, 1);
    }
}
