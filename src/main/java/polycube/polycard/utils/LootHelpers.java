package polycube.polycard.utils;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;

import java.util.List;

public final class LootHelpers {

    private LootHelpers() {}

    /// Builds a one-roll loot pool whose outcomes match CardHelpers.selectRarity.
    public static LootPool.Builder lootPoolFromCardType(CardType cardType) {
        var pool = LootPool.lootPool();
        var rarities = cardType.getRarities();

        int scale = 0;
        for (var rarity : rarities) {
            var scaleCandidate = (int) Math.pow(10, Float.toString(rarity.probability()).length() - 2); // subtract "0."
            scale = Math.max(scale, scaleCandidate);
        }

        for (int i = 0; i < rarities.size(); i++) {
            var rarity = rarities.get(i);
            int cumulativeWeight = (int) (rarity.probability() * scale);
            int nextCumulativeWeight = i + 1 < rarities.size() ? (int) (rarities.get(i + 1).probability() * scale) : 0;
            int rarityWeight = cumulativeWeight - nextCumulativeWeight;

            var cardTemplate = new Card(cardType, rarity.rarityLevel()).getItemTemplate();
            pool.add(lootItemFromTemplate(cardTemplate).setWeight(rarityWeight));
        }

        int emptyWeight = scale - (int) (rarities.getFirst().probability() * scale);
        if (emptyWeight > 0) {
            pool.add(EmptyLootItem.emptyItem().setWeight(emptyWeight));
        }

        return pool;
    }

    /// Converts an ItemStackTemplate to a LootItem.Builder, applying count and component functions as needed.
    public static LootItem.Builder<?> lootItemFromTemplate(ItemStackTemplate template) {
        return LootItem.lootTableItem(template.item().value())
                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(template.count())))
                .apply(() -> new SetComponentsFunction(List.of(), template.components()));
    }
}
