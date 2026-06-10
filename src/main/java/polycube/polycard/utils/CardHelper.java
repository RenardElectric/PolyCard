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
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/// Manages card creation, storage, and cooldowns for the PolyCard mod.
public class CardHelper {
    /// Give a card to a player, creating it with a random rarity level based on the card type's probabilities.
    ///
    /// @param player   The player to give the card to.
    /// @param cardType The type of card to give.
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

    /// Load a player's attributes based on their equipped cards, adding the attribute modifiers from each card to the player.
    ///
    /// @param player The player to load the attributes for.
    public static void loadPlayerAttributes(ServerPlayer player) {
        for(var entry : PolyCard.STORAGE.getPlayerData(player).equippedCardsMap().entrySet()) {
            addCardAttributes(player, new Card(entry.getKey(), entry.getValue()));
        }
    }

    /// Add the attribute modifiers from a card to a player, applying the effects of the card to the player.
    ///
    /// @param player The player to add the attributes to.
    /// @param card   The card to add the attributes from.
    public static void addCardAttributes(ServerPlayer player, Card card) {
        player.getAttributes().addTransientAttributeModifiers(card.getAttributeModifiers());
    }

    /// Remove the attribute modifiers from a card from a player, removing the effects of the card from the player.
    ///
    /// @param player The player to remove the attributes from.
    /// @param card   The card to remove the attributes from.
    public static void removeCardAttributes(ServerPlayer player, Card card) {
        player.getAttributes().removeAttributeModifiers(card.getAttributeModifiers());
    }

    /// Give a specific card to a player.
    ///
    /// @param player The player to give the card to.
    /// @param card   The card to give to the player.
    public static void giveCard(ServerPlayer player, Card card) {
        player.getInventory().placeItemBackInInventory(card.asItem());
        Helpers.playSound(player, SoundEvents.ITEM_PICKUP);
    }

    /// Create a card with a random rarity level based on the card type's probabilities.
    ///
    /// @param card The card type to create.
    /// @return An optional containing the created card, or empty if no rarity level was selected.
    public static Optional<Card> createCard(CardType card) {
        return getRandomRarityLevel(card).map(
                rarityLevel -> new Card(card, rarityLevel)
        );
    }

    /// Get a random rarity level for a card type based on its probabilities.
    ///
    /// @param cardType The card type to get a random rarity level for.
    /// @return An optional containing the random rarity level, or empty if no rarity level was selected.
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
