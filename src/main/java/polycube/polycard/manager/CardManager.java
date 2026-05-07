package polycube.polycard.manager;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.Rarity;
import polycube.polycard.utils.Cooldown;

import java.util.*;
import java.util.function.Function;

public class CardManager {
    private static final String cardIdKey = "card_id";
    private static final String cardRarityKey = "rarity_id";
    private static final Random random = new Random();

    private final Storage storage;
    private final Map<String, Cooldown> cooldowns = new HashMap<>();

    public CardManager() {
        this.storage = Storage.Initialize();
    }

    public boolean isReady(String key, int defaultCooldown) {
        Cooldown cd = cooldowns.get(key);
        if(cd == null){
            cd = new Cooldown(defaultCooldown);
            cooldowns.put(key, cd);
            return true;
        }
        return cd.isReady();
    }

    public void useOrCreate(String key, int defaultCooldown) {
        Cooldown cd = cooldowns.computeIfAbsent(key, k -> new Cooldown(defaultCooldown));
        cd.use();
    }

    public long getRemainingTime(String key) {
        Cooldown cd = cooldowns.get(key);
        if(cd == null){
            return 0;
        }
        return cd.getRemainingTime();
    }

    public Storage getStorage() {
        return storage;
    }

    /**
     * Creates a card ItemStack with a random rarity for the given card type.
     * @param card The card type to create.
     * @return An ItemStack representation of the card with random rarity.
     */
    public static ItemStack createCardItem(Card card) {
        Rarity randomRarity = getRandomRarity(card.getMinRarity());
        return createCardItem(card, randomRarity);
    }

    /**
     * Creates a card ItemStack with a specific rarity for the given card type.
     * @param card The card type to create.
     * @param rarity The rarity level of the card.
     * @return An ItemStack representation of the card with the specified rarity.
     */
    public static ItemStack createCardItem(Card card, Rarity rarity) {
        ItemStack item = new ItemStack(rarity.getItem());
        // Set display name with rarity color
        item.set(DataComponents.ITEM_NAME, Component.literal(rarity.getName() + " " + card.getName()).withStyle(rarity.getColor()));

        // Set lore with descriptions
        item.set(DataComponents.LORE, new ItemLore(card.getDescriptions(rarity).stream().map(desc -> (Component)Component.literal(desc)).toList()));

        // Store stable card id + rarity in metadata for equip detection.
        var customData = item.get(DataComponents.CUSTOM_DATA);
        var customDataTag = customData != null ? customData.copyTag() : new CompoundTag();

        var polyCardTag = new CompoundTag();
        polyCardTag.putString(cardIdKey, card.getId());
        polyCardTag.putString(cardRarityKey, rarity.getId());
        customDataTag.put(PolyCard.MOD_ID, polyCardTag);
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(customDataTag));

        return item;
    }


    /// Checks if an ItemStack is a card.
    ///
    /// @param item The ItemStack to check.
    /// @return True if the item is a card, false otherwise.
    public static boolean isCard(ItemStack item) {
        return getCardType(item).isPresent() && getCardRarity(item).isPresent();
    }

    /// Extracts the card type from a card ItemStack.
    ///
    /// @param item The card ItemStack.
    /// @return The Cards enum value, or null if the item is not a card.
    public static Optional<Card> getCardType(ItemStack item) {
        return getCardData(item, cardIdKey, Card::fromId);
    }

    /// Extracts the rarity from a card ItemStack.
    ///
    /// @param item The card ItemStack.
    /// @return The Rarity enum value, or null if the item is not a card.
    public static Optional<Rarity> getCardRarity(ItemStack item) {
        return getCardData(item, cardRarityKey, Rarity::fromId);
    }

    /// Generic method to extract card data from an ItemStack's custom data.
    ///
    /// @param item The ItemStack to extract data from.
    /// @param key The key in the custom data to look for.
    /// @param fromId A function that converts a string ID to the desired type, returning an Optional.
    /// @return An Optional containing the extracted data, or empty if not found or invalid.
    private static <T> Optional<T> getCardData(ItemStack item, String key, Function<String, Optional<T>> fromId) {
        var customData = item.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return Optional.empty();

        var polyCardData = customData.copyTag().getCompound(PolyCard.MOD_ID);
        if (polyCardData.isEmpty()) return Optional.empty();

        var rarityId = polyCardData.get().getString(key);
        return rarityId.flatMap(fromId);
    }

    /// Gets a random rarity with probability based on rarity tier, respecting the minimum rarity.
    ///
    /// @param minRarity The minimum rarity for this card.
    /// @return A random rarity at or above the minimum rarity.
    public static Rarity getRandomRarity(Rarity minRarity) {
        int roll = random.nextInt(100);
        Rarity result;

        // Common: 50%, Uncommon: 25%, Rare: 15%, Epic: 9%, Legendary: 1%
        if (roll < 50) result = Rarity.COMMON;
        else if (roll < 75) result = Rarity.UNCOMMON;
        else if (roll < 90) result = Rarity.RARE;
        else if (roll < 99) result = Rarity.EPIC;
        else result = Rarity.LEGENDARY;
        // If the rolled rarity is below the minimum, return minimum
        if (result.ordinal() < minRarity.ordinal()) {
            return minRarity;
        }
        return result;
    }
//
//    /**
//     * Initializes and returns a list of all card listeners.
//     * @return A list of CardListener instances for all cards.
//     */
//    public List<CardListener> initializeCardsListener() {
//        List<CardListener> cardListeners = new ArrayList<>();
//        cardListeners.add(new CowListener(plugin,this));
//        cardListeners.add(new EndermanListener(plugin,this));
//        cardListeners.add(new IronGolemListener(plugin,this));
//
//        return cardListeners;
//    }
}
