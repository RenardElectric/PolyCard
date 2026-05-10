package polycube.polycard.manager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityType;
import polycube.polycard.utils.Cooldowns;

import java.util.Optional;
import java.util.Random;

/// Manages card creation, storage, and cooldowns for the PolyCard mod.
public class CardManager {
    private static final Random random = new Random();
    private final Cooldowns cooldowns = new Cooldowns();
    private Storage storage = null;

    public CardManager() {
        PolyCard.runTaskTimer(20, 20, _ -> cooldowns.tick(20));
    }

    /// Load the storage for this card manager from the server's saved data.
    ///
    /// @param server The server to load the storage from.
    public void load(MinecraftServer server) {
        storage = Storage.getSavedStorage(server);
        PolyCard.debug("Storage loaded with {} players.", storage.playerDataMap.size());
    }

    /// Mark the storage as dirty to save it on the next server tick.
    public void save() {
        PolyCard.debug("Marking storage as dirty for saving.");
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

    /// Give a card to a player and send them a message about it.
    ///
    /// @param player The player to give the card to.
    /// @param cardType The type of card to give.
    public static void giveCard(ServerPlayer player, CardType cardType) {
        CardManager.createCard(cardType).ifPresent(
                card -> {
                    player.getInventory().add(card.asItem());
                    PolyCard.debug("{} received a card: {}", player.getName(), card);
                    player.sendSystemMessage(
                            Component.literal("✨ You found a ")
                                    .append(card.getFormatedName())
                                    .append(" card!")
                                    .withStyle(ChatFormatting.GREEN)
                    );
                }
        );

    }

    /// Create a card with a random rarity based on the card type's probabilities.
    ///
    /// @param card The card type to create.
    /// @return An optional containing the created card, or empty if no rarity was selected.
    public static Optional<Card> createCard(CardType card) {
        return getRandomRarity(card).map(
                randomRarity -> new Card(card, randomRarity)
        );
    }

    /// Get a random rarity for a card type based on its probabilities.
    ///
    /// @param cardType The card type to get a random rarity for.
    /// @return An optional containing the random rarity, or empty if no rarity was selected.
    public static Optional<RarityType> getRandomRarity(CardType cardType) {
        if (random.nextInt(100) < cardType.getProbability(RarityType.LEGENDARY)) return Optional.of(RarityType.LEGENDARY);
        else if (random.nextInt(100) < cardType.getProbability(RarityType.EPIC)) return Optional.of(RarityType.EPIC);
        else if (random.nextInt(100) < cardType.getProbability(RarityType.RARE)) return Optional.of(RarityType.RARE);
        else if (random.nextInt(100) < cardType.getProbability(RarityType.UNCOMMON)) return Optional.of(RarityType.UNCOMMON);
        else if (random.nextInt(100) < cardType.getProbability(RarityType.COMMON)) return Optional.of(RarityType.COMMON);
        return Optional.empty();
    }
}
