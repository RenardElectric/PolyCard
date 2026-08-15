package polycube.polycard.client;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.predicates.entity.PlayerPredicate;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardGroup;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/// Fabric data-generation entrypoint for generated advancements and layered card item models.
public class DataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(MyAdvancementProvider::new);
        pack.addProvider(MyModelProvider::new);
    }

    public static class MyAdvancementProvider extends FabricAdvancementProvider {
        protected MyAdvancementProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
            super(output, registryLookup);
        }

        @Override
        public void generateAdvancement(HolderLookup.Provider wrapperLookup, Consumer<AdvancementHolder> consumer) {
            var iconItem = new ItemStackTemplate(
                    Card.CARD_ITEM,
                    DataComponentPatch.builder()
                            .set(DataComponents.ITEM_MODEL, Identifier.parse(PolyCard.MOD_ID + ":polycard_icon"))
                            .build()
            );

            var rootAdvancementBuilder = Advancement.Builder.advancement()
                    .display(
                            iconItem,
                            Component.literal("PolyCard"),
                            Component.literal("Collect all cards in the PolyCard mod"),
                            Identifier.withDefaultNamespace("block/amethyst_block"),
                            AdvancementType.CHALLENGE,
                            true,
                            true,
                            false
                    );
            addCompletionCriteria(rootAdvancementBuilder);
            AdvancementHolder rootAdvancement = rootAdvancementBuilder.save(consumer, Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, "root"));

            Map<CardGroup, AdvancementHolder> groupAdvancements = new EnumMap<>(CardGroup.class);
            for (var cardGroup : CardGroup.values()) {
                var identifier = cardGroup.getId();
                var groupAdvancementBuilder = Advancement.Builder.advancement()
                        .parent(rootAdvancement)
                        .display(
                                new ItemStackTemplate(
                                        Card.CARD_ITEM,
                                        DataComponentPatch.builder()
                                                .set(DataComponents.ITEM_MODEL, identifier)
                                                .build()
                                ),
                                Component.literal(cardGroup + " Cards"),
                                Component.literal("Collect all " + cardGroup + " Cards"),
                                null,
                                AdvancementType.CHALLENGE,
                                true,
                                true,
                                false
                        );
                addCompletionCriteria(groupAdvancementBuilder, cardGroup);
                var groupAdvancement = groupAdvancementBuilder.save(consumer, identifier);

                groupAdvancements.put(cardGroup, groupAdvancement);
            }

            for (var cardType : CardType.values())
                addType(cardType, groupAdvancements, consumer);
        }

        private static void addCompletionCriteria(Advancement.Builder advancement) {
            for (var cardGroup : CardGroup.values()) {
                advancement.addCriterion(cardGroup.getSerializedName(), cardUnlockedCriterion(cardGroup.getId()));
            }
        }

        private static void addCompletionCriteria(Advancement.Builder advancement, CardGroup cardGroup) {
            for (var cardType : CardType.values()) {
                if (cardType.getGroup() != cardGroup) continue;
                advancement.addCriterion(cardType.getSerializedName(), cardUnlockedCriterion(cardType.getId()));
            }
        }

        private static void addCompletionCriteria(Advancement.Builder advancement, CardType cardType) {
            for (var rarity : cardType.getRarities()) {
                var rarityLevel = rarity.rarityLevel();
                var cardAdvancementId = new Card(cardType, rarityLevel).getId();
                advancement.addCriterion(
                        cardType.getSerializedName() + "_" + rarityLevel.getSerializedName(),
                        cardUnlockedCriterion(cardAdvancementId)
                );
            }
        }

        private static Criterion<PlayerTrigger.TriggerInstance> cardUnlockedCriterion(Identifier cardAdvancementId) {
            var playerPredicate = PlayerPredicate.Builder.player()
                    .checkAdvancementDone(cardAdvancementId, true)
                    .build();

            return PlayerTrigger.TriggerInstance.located(EntityPredicate.Builder.entity().player(playerPredicate));
        }

        public void addType(CardType cardType, Map<CardGroup, AdvancementHolder> groupAdvancements, Consumer<AdvancementHolder> consumer) {
            var parent = groupAdvancements.get(cardType.getGroup());
            var identifier = cardType.getId();
            var cardTypeAdvancementBuilder = Advancement.Builder.advancement()
                    .parent(parent)
                    .display(
                            new ItemStackTemplate(
                                    Card.CARD_ITEM,
                                    DataComponentPatch.builder()
                                            .set(DataComponents.ITEM_MODEL, identifier)
                                            .build()
                            ),
                            Component.literal(cardType + " Cards"),
                            Component.literal("Collect all " + cardType + " Cards by " + cardType.getCondition()),
                            null,
                            AdvancementType.GOAL,
                            true,
                            true,
                            false
                    );
            addCompletionCriteria(cardTypeAdvancementBuilder, cardType);
            parent = cardTypeAdvancementBuilder.save(consumer, identifier);

            for (var rarity : cardType.getRarities()) {

                var rarityLevel = rarity.rarityLevel();
                var card = new Card(cardType, rarity.rarityLevel());
                identifier = card.getId();
                var item = new ItemStackTemplate(
                        Card.CARD_ITEM,
                        DataComponentPatch.builder()
                                .set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, card.isEnchanted())
                                .set(DataComponents.ITEM_MODEL, identifier)
                                .build()
                );

                parent = Advancement.Builder.advancement()
                        .parent(parent)
                        .display(
                                item,
                                Component.literal(card.toString()),
                                Component.literal("Collect the ").append(Component.literal(card.toString()).withStyle(rarityLevel.color())),
                                null,
                                rarityLevel == RarityLevel.LEGENDARY ? AdvancementType.CHALLENGE : AdvancementType.TASK,
                                true,
                                rarityLevel == RarityLevel.LEGENDARY,
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

            Advancement.Builder.advancement()
                    .parent(parent)
                    .addCriterion("tick", PlayerTrigger.TriggerInstance.tick())
                    .save(consumer, cardType.getId().withSuffix("/end"));
        }
    }

    public static class MyModelProvider extends FabricModelProvider {
        public MyModelProvider(FabricPackOutput output) {
            super(output);
        }

        @Override
        public void generateBlockStateModels(BlockModelGenerators blockModelGenerators) {}

        @Override
        public void generateItemModels(ItemModelGenerators itemModelGenerators) {
            for (var cardGroup : CardGroup.values()) {
                itemModelGenerators.generateFlatItem(PolyCardClient.getDatagenItem(cardGroup), ModelTemplates.FLAT_ITEM);
            }

            for (var rarityLevel : RarityLevel.values()) {
                var rarityId = rarityLevel.getId().withPrefix("item/");
                ModelTemplates.FLAT_ITEM.create(rarityId, TextureMapping.layer0(new Material(rarityId)), itemModelGenerators.modelOutput);
            }

            for (var cardType : CardType.values()) {
                var cardTypeId = cardType.getId().withPrefix("item/");
                ModelTemplates.FLAT_ITEM.create(cardTypeId, TextureMapping.layer0(new Material(cardTypeId)), itemModelGenerators.modelOutput);
                itemModelGenerators.itemModelOutput.accept(PolyCardClient.getDatagenItem(cardType), ItemModelUtils.plainModel(cardTypeId));
            }

            for (var cardType : CardType.values()) {
                for (var rarity : cardType.getRarities()) {
                    var rarityLevel = rarity.rarityLevel();

                    ItemModel.Unbaked rarityModel = ItemModelUtils.plainModel(rarityLevel.getId().withPrefix("item/"));
                    ItemModel.Unbaked cardTypeModel = ItemModelUtils.plainModel(cardType.getId().withPrefix("item/"));

                    itemModelGenerators.itemModelOutput.accept(
                            PolyCardClient.getDatagenItem(new Card(cardType, rarityLevel)),
                            ItemModelUtils.composite(rarityModel, cardTypeModel)
                    );
                }
            }
        }
    }
}
