package polycube.polycard.manager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.utils.Cooldowns;
import polycube.polycard.utils.Helpers;

import java.util.*;

/// Manages card creation, storage, and cooldowns for the PolyCard mod.
public class CardManager {
    private static final Random random = new Random();
    private final Cooldowns cooldowns = new Cooldowns();
    private Storage storage = null;

    public CardManager() {
        Helpers.runTaskTimer(20, 20, _ -> cooldowns.tick(20));
    }

    /// Load the storage for this card manager from the server's saved data.
    ///
    /// @param server The server to load the storage from.
    public void load(MinecraftServer server) {
        storage = Storage.getSavedStorage(server);
        Helpers.debug("Storage loaded.");
    }

    /// Mark the storage as dirty to save it on the next server tick.
    public void save() {
        if (storage == null) {
            Helpers.debug("Attempted to save card storage before it was loaded.");
            return;
        }

        Helpers.debug("Marking storage as dirty for saving.");
        storage.setDirty();
    }

    /// Get the storage for this card manager.
    ///
    /// @return The storage for this card manager.
    public Storage getStorage() {
        return storage;
    }

    /// Get the cooldown manager for this card manager.
    ///
    /// @return The cooldown manager for this card manager.
    public Cooldowns getCooldowns() {
        return cooldowns;
    }

    /// Give a card to a player, creating it with a random rarity level based on the card type's probabilities.
    ///
    /// @param player The player to give the card to.
    /// @param cardType The type of card to give.
    public static void receiveCard(ServerPlayer player, CardType cardType) {
        CardManager.createCard(cardType).ifPresent(
                card -> {
                    giveCard(player, card);
                    Helpers.debug("{} received a card: {}", player.getName(), card);
                    player.sendSystemMessage(
                            Component.literal("✨ You found a ")
                                    .append(card.getFormattedName())
                                    .append(" card!")
                                    .withStyle(ChatFormatting.GREEN)
                    );
//                    if (card.rarityLevel() == RarityLevel.LEGENDARY) {
//                        //noinspection resource
//                        var server = player.level().getServer();
//                        server.sendSystemMessage(
//                                Component.literal("🎉 ")
//                                        .append(player.getDisplayName())
//                                        .append(" found a ")
//                                        .append(card.getFormattedName())
//                                        .append(" card! 🎉")
//                                        .withStyle(ChatFormatting.GOLD)
//                        );
//                        PolyCard.playSound(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE);
//                    }
                }
        );
    }

    /// Load a player's attributes based on their equipped cards, adding the attribute modifiers from each card to the player.
    ///
    /// @param player The player to load the attributes for.
    public void loadPlayerAttributes(ServerPlayer player) {
        var playerCards = storage.data(player).getEquippedCards();
        playerCards.forEach(card -> addCardAttributes(player, card));
    }

    /// Add the attribute modifiers from a card to a player, applying the effects of the card to the player.
    ///
    /// @param player The player to add the attributes to.
    /// @param card The card to add the attributes from.
    public static void addCardAttributes(ServerPlayer player, Card card) {
        player.getAttributes().addTransientAttributeModifiers(card.getAttributeModifiers());
    }

    /// Remove the attribute modifiers from a card from a player, removing the effects of the card from the player.
    ///
    /// @param player The player to remove the attributes from.
    /// @param card The card to remove the attributes from.
    public static void removeCardAttributes(ServerPlayer player, Card card) {
        player.getAttributes().removeAttributeModifiers(card.getAttributeModifiers());
    }

    /// Give a specific card to a player.
    ///
    /// @param player The player to give the card to.
    /// @param card The card to give to the player.
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
        var rarities = RarityLevel.values();
        Collections.reverse(Arrays.asList(rarities));
        for (var rarityLevel : rarities) {
            if (rollsUnder(cardType.getProbability(rarityLevel))) {
                return Optional.of(rarityLevel);
            }
        }
        return Optional.empty();
    }

    private static boolean rollsUnder(int probability) {
        if (probability <= 0) {
            return false;
        }

        return random.nextInt(100) < Math.min(probability, 100);
    }
}
