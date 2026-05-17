package polycube.polycard;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class DataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(AdvancementProvider::new);
    }

    public static class AdvancementProvider extends FabricAdvancementProvider {
        protected AdvancementProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
            super(output, registryLookup);
        }

        @Override
        public void generateAdvancement(HolderLookup.@NonNull Provider wrapperLookup, @NonNull Consumer<AdvancementHolder> consumer) {
            var icon_item = new ItemStackTemplate(
                    Items.KNOWLEDGE_BOOK,
                    DataComponentPatch.builder()
                            .set(DataComponents.ITEM_MODEL, Identifier.parse(PolyCard.MOD_ID + ":polycard_icon"))
                            .build()
            );

            var modData = FabricLoader.getInstance()
                    .getModContainer(PolyCard.MOD_ID)
                    .map(ModContainer::getMetadata)
                    .orElseThrow();

            AdvancementHolder getDirt = Advancement.Builder.advancement()
                    .display(
                            icon_item,
                            Component.literal(modData.getName()),
                            Component.literal(modData.getDescription()),
                            Identifier.withDefaultNamespace("block/amethyst_block"),
                            AdvancementType.TASK,
                            false,
                            false,
                            false
                    )
                    .addCriterion("tick", PlayerTrigger.TriggerInstance.tick())
                    .save(consumer, Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, "polycard_root"));

            for (var cardType : CardType.values()) {
                addType(cardType, getDirt, consumer);
            }
        }

        public void addType(CardType cardType, AdvancementHolder parent, Consumer<AdvancementHolder> consumer) {
            for (var rarity : cardType.getRarities()) {

                var rarityLevel = rarity.rarityLevel();
                var card = new Card(cardType, rarity.rarityLevel());
                var identifier = Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, cardType.getSerializedName() + "/" + rarity.rarityLevel().getSerializedName());
                var item = new ItemStackTemplate(
                        Items.KNOWLEDGE_BOOK,
                        DataComponentPatch.builder()
                                .set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, cardType.isEnchanted(rarityLevel))
                                .set(DataComponents.ITEM_MODEL, identifier)
                                .build()
                );

                parent = Advancement.Builder.advancement()
                        .parent(parent)
                        .display(
                                item,
                                Component.literal(card.toString()),
                                Component.literal("Unlock the ")
                                        .append(Component.literal(card.toString()).withStyle(rarityLevel.color()))
                                        .append(" by " + cardType.getCondition()),
                                null,
                                rarityLevel.equals(RarityLevel.LEGENDARY) ? AdvancementType.CHALLENGE : AdvancementType.TASK,
                                true,
                                rarityLevel.equals(RarityLevel.LEGENDARY),
                                false
                        )
                        .addCriterion(
                                "got_card",
                                InventoryChangeTrigger.TriggerInstance.hasItems(
                                        ItemPredicate.Builder.item()
                                                .withComponents(
                                                        DataComponentMatchers.Builder.components().exact(
                                                                DataComponentExactPredicate.expect(
                                                                        DataComponents.ITEM_MODEL,
                                                                        identifier
                                                                )
                                                        ).build()
                                                )
                                                .build()
                                )
                        )
                        .save(consumer, identifier);
            }
        }
    }
}
