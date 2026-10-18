package polycube.polycard.utils;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.config.DropRatesConfig;

import java.util.ArrayList;
import java.util.Optional;

public final class LootHelpers {

    private LootHelpers() {}

    /// Tries the highest rarity first, giving the same outcomes as independent rarity rolls.
    public static LootPool.Builder lootPoolFromCardType(CardType cardType) {
        var entries = new ArrayList<LootPoolEntryContainer.Builder<?>>();
        for (var rarity : DropRatesConfig.lootDistribution(cardType).rarities().reversed()) {
            if (rarity.probability() == 0.0F) continue;
            var card = new Card(cardType, rarity.rarityLevel());
            entries.add(lootItemFromTemplate(card.getItemTemplate())
                    .when(LootItemRandomChanceCondition.randomChance(rarity.probability())));
        }
        entries.add(EmptyLootItem.emptyItem());
        return LootPool.lootPool().add(AlternativesEntry.alternatives(entries, entry -> entry));
    }

    /// Converts an ItemStackTemplate to a LootItem.Builder, applying count and component functions as needed.
    public static LootItem.Builder<?> lootItemFromTemplate(ItemStackTemplate template) {
        return LootItem.lootTableItem(template.item().value())
                .apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(template.count())))
                .apply(() -> new SetComponentsFunction(Optional.empty(), template.components()));
    }
}
