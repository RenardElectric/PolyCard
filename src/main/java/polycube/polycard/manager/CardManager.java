package polycube.polycard.manager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityType;
import polycube.polycard.utils.Cooldown;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class CardManager {
    private static final Random random = new Random();

    private Storage storage = null;
    private final Map<String, Cooldown> cooldowns = new HashMap<>();

    public void load(MinecraftServer server) {
        storage = Storage.getSavedStorage(server);
        PolyCard.debug("Storage loaded with {} players.", storage.playerDataMap.size());
    }

    public void save() {
        PolyCard.debug("Marking storage as dirty for saving.");
        storage.setDirty();
    }

    public boolean isReady(String key, int defaultCooldown) {
        Cooldown cd = cooldowns.get(key);
        if (cd == null) {
            cd = new Cooldown(defaultCooldown);
            cooldowns.put(key, cd);
            return true;
        }
        return cd.isReady();
    }

    public void useOrCreate(String key, int defaultCooldown) {
        Cooldown cd = cooldowns.computeIfAbsent(key, _ -> new Cooldown(defaultCooldown));
        cd.use();
    }

    public long getRemainingTime(String key) {
        Cooldown cd = cooldowns.get(key);
        if (cd == null) {
            return 0;
        }
        return cd.getRemainingTime();
    }

    public Storage getStorage() {
        return storage;
    }

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

    public static Optional<Card> createCard(CardType card) {
        return getRandomRarity(card).map(
                randomRarity -> new Card(card, randomRarity)
        );
    }

    /// Gets a random rarity with probability based on rarity tier, respecting the minimum rarity.
    ///
    /// @param cardType The card type to get a random rarity for.
    /// @return A random rarity at or above the minimum rarity.
    public static Optional<RarityType> getRandomRarity(CardType cardType) {
        Optional<RarityType> result = Optional.empty();

        if (random.nextInt(100) < cardType.getProbability(RarityType.COMMON)) result = Optional.of(RarityType.COMMON);
        if (random.nextInt(100) < cardType.getProbability(RarityType.UNCOMMON))
            result = Optional.of(RarityType.UNCOMMON);
        if (random.nextInt(100) < cardType.getProbability(RarityType.RARE)) result = Optional.of(RarityType.RARE);
        if (random.nextInt(100) < cardType.getProbability(RarityType.EPIC)) result = Optional.of(RarityType.EPIC);
        if (random.nextInt(100) < cardType.getProbability(RarityType.LEGENDARY))
            result = Optional.of(RarityType.LEGENDARY);
        return result;
    }
}
