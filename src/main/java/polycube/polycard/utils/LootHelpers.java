package polycube.polycard.utils;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;

import java.util.Optional;

public final class LootHelpers {

    private LootHelpers() {}

    /// Builds a one-roll loot pool whose outcomes match CardHelpers.selectRarity.
    public static LootPool.Builder lootPoolFromCardType(CardType cardType) {
        var pool = LootPool.lootPool();
        for (var outcome : cardType.getRarityDistribution().weightedOutcomes()) {
            outcome.rarityLevel().ifPresentOrElse(
                    rarityLevel -> {
                        Card.tryCreate(cardType, rarityLevel).map(Card::getItemTemplate).ifPresent(
                                cardTemplate -> pool.add(lootItemFromTemplate(cardTemplate).setWeight(outcome.weight()))
                        );
                    },
                    () -> pool.add(EmptyLootItem.emptyItem().setWeight(outcome.weight()))
            );
        }

        return pool;
    }

    /// Converts an ItemStackTemplate to a LootItem.Builder, applying count and component functions as needed.
    public static LootItem.Builder<?> lootItemFromTemplate(ItemStackTemplate template) {
        return LootItem.lootTableItem(template.item().value())
                .apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(template.count())))
                .apply(() -> new SetComponentsFunction(Optional.empty(), template.components()));
    }
}
