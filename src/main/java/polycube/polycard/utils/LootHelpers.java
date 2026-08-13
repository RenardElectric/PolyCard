package polycube.polycard.utils;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;

public final class LootHelpers {
    private static final int PROBABILITY_WEIGHT_SCALE = 1_000_000;

    private LootHelpers() {}

    /// Builds a one-roll loot pool whose outcomes match CardHelpers.selectRarity.
    public static LootPool.Builder lootPoolFromCardType(CardType cardType) {
        var pool = LootPool.lootPool();
        var rarities = cardType.getRarities();

        for (int i = 0; i < rarities.size(); i++) {
            var rarity = rarities.get(i);
            int cumulativeWeight = probabilityToWeight(rarity.probability());
            int nextCumulativeWeight = i + 1 < rarities.size()
                    ? probabilityToWeight(rarities.get(i + 1).probability())
                    : 0;
            int rarityWeight = cumulativeWeight - nextCumulativeWeight;

            var cardTemplate = new Card(cardType, rarity.rarityLevel()).getItemTemplate();
            pool.add(lootItemFromTemplate(cardTemplate).setWeight(rarityWeight));
        }

        int emptyWeight = PROBABILITY_WEIGHT_SCALE - probabilityToWeight(rarities.getFirst().probability());
        if (emptyWeight > 0) {
            pool.add(EmptyLootItem.emptyItem().setWeight(emptyWeight));
        }

        return pool;
    }

    /// Converts an ItemStackTemplate to a LootItem.Builder, applying count and component functions as needed.
    public static LootItem.Builder<?> lootItemFromTemplate(ItemStackTemplate template) {
        LootItem.Builder<?> entry = LootItem.lootTableItem(template.item().value());

        // Count
        if (template.count() != 1)
            entry.apply(SetItemCountFunction.setCount(ConstantValue.exactly(template.count())));

        // All components
        for (var componentEntry : template.components().entrySet()) {
            DataComponentType<?> type = componentEntry.getKey();
            componentEntry.getValue().ifPresent(value -> entry.apply(setComponentUnchecked(type, value)));
        }

        return entry;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static LootItemConditionalFunction.Builder<?> setComponentUnchecked(DataComponentType<?> type, Object value) {
        return SetComponentsFunction.setComponent((DataComponentType) type, value);
    }

    private static int probabilityToWeight(float probability) {
        return Math.round(probability * PROBABILITY_WEIGHT_SCALE);
    }
}
