package polycube.polycard.client;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
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
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/// This class is responsible for generating the advancements for the mod.
/// It uses the Fabric Data Generator API to create advancements based on the card types and rarities defined in the mod.
public class DataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(AdvancementProvider::new);
        pack.addProvider(ModelProvider::new);
    }

    public static class AdvancementProvider extends FabricAdvancementProvider {
        protected AdvancementProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
            super(output, registryLookup);
        }

        @Override
        public void generateAdvancement(HolderLookup.@NonNull Provider wrapperLookup, @NonNull Consumer<AdvancementHolder> consumer) {
            var icon_item = new ItemStackTemplate(
                    Card.CARD_ITEM,
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
                var identifier = Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, cardType.getCardGroup() + "/" + cardType.getSerializedName() + "/" + rarityLevel.getSerializedName());
                var item = new ItemStackTemplate(
                        Card.CARD_ITEM,
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

    public static class ModelProvider extends FabricModelProvider {
        public ModelProvider(FabricPackOutput output) {
            super(output);
        }

        @Override
        public void generateBlockStateModels(@NonNull BlockModelGenerators blockModelGenerators) {

        }

        @Override
        public void generateItemModels(@NonNull ItemModelGenerators itemModelGenerators) {
            for (var rarityLevel : RarityLevel.values()) {
                var rarityId = Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, "item/" + rarityLevel.getSerializedName());
                ModelTemplates.FLAT_ITEM.create(rarityId, TextureMapping.layer0(new Material(rarityId)), itemModelGenerators.modelOutput);
            }

            for (var cardType : CardType.values()) {
                var cardTypeId = Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, "item/" + cardType.getCardGroup() + "/" + cardType.getSerializedName());
                ModelTemplates.FLAT_ITEM.create(cardTypeId, TextureMapping.layer0(new Material(cardTypeId)), itemModelGenerators.modelOutput);
            }

            for (var cardType : CardType.values()) {
                for (var rarity : cardType.getRarities()) {
                    var rarityLevel = rarity.rarityLevel();
                    var rarityId = Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, "item/" + rarityLevel.getSerializedName());
                    var cardTypeId = Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, "item/" + cardType.getCardGroup() + "/" + cardType.getSerializedName());

                    ItemModel.Unbaked hoe = ItemModelUtils.plainModel(rarityId);
                    ItemModel.Unbaked hoePlus = ItemModelUtils.plainModel(cardTypeId);

                    itemModelGenerators.itemModelOutput.accept(
                            PolyCardClient.CARD_ITEM_MAP.get(new Card(cardType, rarityLevel)),
                            ItemModelUtils.composite(hoe, hoePlus)
                    );
                }
            }

        }
    }
}
