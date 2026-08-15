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
    private static final Map<CardType, Item> DATAGEN_CARDTYPE_ITEMS = new HashMap<>();

    @Override
    public void onInitializeClient() {
        if (System.getProperty("fabric-api.datagen") == null) {
            PolyCard.LOGGER.debug("Skipping datagen-only card item registration");
            return;
        }

        for (var cardGroup : CardGroup.values())
            DATAGEN_CARDGROUP_ITEMS.put(cardGroup, registerDatagenItem(cardGroup));

        for (var cardType : CardType.values()) {
            DATAGEN_CARDTYPE_ITEMS.put(cardType, registerDatagenItem(cardType));

            for (var rarity : cardType.getRarities()) {
                var rarityLevel = rarity.rarityLevel();
                Card card = new Card(cardType, rarityLevel);
                DATAGEN_CARD_ITEMS.put(card, registerDatagenItem(card));
            }
        }
        PolyCard.LOGGER.debug("Registered {} placeholder card items for data generation", DATAGEN_CARD_ITEMS.size());
        PolyCard.LOGGER.debug("Registered {} placeholder card group items for data generation", DATAGEN_CARDGROUP_ITEMS.size());
        PolyCard.LOGGER.debug("Registered {} placeholder card type items for data generation", DATAGEN_CARDTYPE_ITEMS.size());
    }

    static Item getDatagenItem(Card card) {
        return Objects.requireNonNull(DATAGEN_CARD_ITEMS.get(card), "Missing datagen item for " + card);
    }

    static Item getDatagenItem(CardGroup cardGroup) {
        return Objects.requireNonNull(DATAGEN_CARDGROUP_ITEMS.get(cardGroup), "Missing datagen item for " + cardGroup);
    }

    static Item getDatagenItem(CardType cardType) {
        return Objects.requireNonNull(DATAGEN_CARDTYPE_ITEMS.get(cardType), "Missing datagen item for " + cardType);
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

    private static Item registerDatagenItem(CardType cardType) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, cardType.getId());
        Item item = new Item(new Item.Properties().setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        return item;
    }
}
