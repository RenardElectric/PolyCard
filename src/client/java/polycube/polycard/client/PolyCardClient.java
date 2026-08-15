package polycube.polycard.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardGroup;
import polycube.polycard.card.CardType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/// Registers placeholder card items used only while generating item-model JSON.
public class PolyCardClient implements ClientModInitializer {
    private static final Map<Card, Item> DATAGEN_CARD_ITEMS = new HashMap<>();
    private static final Map<CardGroup, Item> DATAGEN_CARDGROUP_ITEMS = new HashMap<>();

    @Override
    public void onInitializeClient() {
        if (System.getProperty("fabric-api.datagen") == null) {
            PolyCard.LOGGER.debug("Skipping datagen-only card item registration");
            return;
        }

        for (var cardGroup : CardGroup.values()) {
            Item item = registerDatagenItem(cardGroup);
            DATAGEN_CARDGROUP_ITEMS.put(cardGroup, item);
        }

        for (var cardType : CardType.values()) {
            for (var rarity : cardType.getRarities()) {
                var rarityLevel = rarity.rarityLevel();
                Card card = new Card(cardType, rarityLevel);
                Item item = registerDatagenItem(card);
                DATAGEN_CARD_ITEMS.put(card, item);
            }
        }
        PolyCard.LOGGER.debug("Registered {} placeholder card items for data generation", DATAGEN_CARD_ITEMS.size());
        PolyCard.LOGGER.debug("Registered {} placeholder card group items for data generation", DATAGEN_CARDGROUP_ITEMS.size());
    }

    static Item getDatagenItem(Card card) {
        return Objects.requireNonNull(DATAGEN_CARD_ITEMS.get(card), "Missing datagen item for " + card);
    }

    static Item getDatagenItem(CardGroup cardGroup) {
        return Objects.requireNonNull(DATAGEN_CARDGROUP_ITEMS.get(cardGroup), "Missing datagen item for " + cardGroup);
    }

    private static Item registerDatagenItem(Card card) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, card.getId());
        Item item = new Item(new Item.Properties().setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        return item;
    }

    private static Item registerDatagenItem(CardGroup cardGroup) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, cardGroup.getId());
        Item item = new Item(new Item.Properties().setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        return item;
    }
}
