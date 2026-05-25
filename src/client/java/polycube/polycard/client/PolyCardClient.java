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

public class PolyCardClient implements ClientModInitializer {
	public static final Map<Card, Item> CARD_ITEM_MAP = new HashMap<>();

	@Override
	public void onInitializeClient() {
		for (var cardType : CardType.values()) {
			for (var rarity : cardType.getRarities()) {
				var rarityLevel = rarity.rarityLevel();
				Card card = new Card(cardType, rarityLevel);
				Item item = register(card);
				CARD_ITEM_MAP.put(card, item);
			}
		}
	}

	public static Item register(Card card) {
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, card.getId()));
		Item item = new Item(new Item.Properties().setId(itemKey));
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);
		return item;
	}
}
