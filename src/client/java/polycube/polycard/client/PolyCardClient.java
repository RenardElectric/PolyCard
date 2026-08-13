package polycube.polycard.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/// Registers placeholder card items used only while generating item-model JSON.
public class PolyCardClient implements ClientModInitializer {
    private static final String DATAGEN_PROPERTY = "fabric-api.datagen";
    private static final Map<Card, Item> DATAGEN_CARD_ITEMS = new HashMap<>();

    @Override
    public void onInitializeClient() {
        if (System.getProperty(DATAGEN_PROPERTY) == null) {
            PolyCard.LOGGER.debug("Skipping datagen-only card item registration");
            return;
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
    }

    static Item getDatagenItem(Card card) {
        return Objects.requireNonNull(DATAGEN_CARD_ITEMS.get(card), "Missing datagen item for " + card);
    }

    private static Item registerDatagenItem(Card card) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, card.getId()));
        Item item = new Item(new Item.Properties().setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        return item;
    }
}
