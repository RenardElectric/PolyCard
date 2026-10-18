package polycube.polycard.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.Rarity;
import polycube.polycard.card.RarityDistribution;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.config.DropRateSettings;
import polycube.polycard.data.Equipment;
import polycube.polycard.data.PlayerData;
import polycube.polycard.data.Storage;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class CardDomainGameTests {
    @GameTest
    public void everySupportedCardRoundTripsThroughItsItem(GameTestHelper helper) {
        for (var cardType : CardType.values()) {
            for (var rarity : cardType.getRarities()) {
                var card = new Card(cardType, rarity.rarityLevel());
                var stack = card.asItem();
                helper.assertTrue(Card.getCard(stack).filter(card::equals).isPresent(),
                        card + " must round-trip through its item payload");
                helper.assertTrue(stack.getMaxStackSize() == 64,
                        card + " must remain stackable to 64");
                helper.assertTrue(stack.get(DataComponents.CONSUMABLE) == null
                                && stack.get(DataComponents.FOOD) == null,
                        card + " must not retain poisonous-potato consumption behavior");
                helper.assertTrue(card.getId().equals(stack.get(DataComponents.ITEM_MODEL)),
                        card + " must select its matching item model");
            }
        }
        helper.succeed();
    }

    @GameTest
    public void forgedAndUnsupportedCardsAreRejected(GameTestHelper helper) {
        var canonical = new Card(CardType.CHICKEN, RarityLevel.COMMON).asItem();
        var forged = new ItemStack(Items.STONE);
        forged.set(DataComponents.CUSTOM_DATA, canonical.get(DataComponents.CUSTOM_DATA));

        helper.assertTrue(Card.getCard(forged).isEmpty(),
                "PolyCard data on another base item must not create a card");
        helper.assertTrue(Card.getCard(Card.CARD_ITEM.getDefaultInstance()).isEmpty(),
                "The carrier item without a complete payload must not be a card");
        helper.assertTrue(Card.tryCreate(CardType.NETHER, RarityLevel.COMMON).isEmpty(),
                "A card type must reject unsupported rarities");
        helper.succeed();
    }

    @GameTest
    public void raritySelectionUsesIndependentRolls(GameTestHelper helper) {
        var selected = CardType.CHICKEN.getRarityDistribution().select(
                CardType.CHICKEN,
                null,
                new SequenceRandom(0.10F, 0.20F, 0.01F, 0.50F, 0.50F)
        );

        helper.assertTrue(selected.filter(rarity -> rarity == RarityLevel.RARE).isPresent(),
                "A later successful rarity roll must replace an earlier successful roll");
        helper.succeed();
    }

    @GameTest
    public void rarityRollsMatchIndependentProbabilities(GameTestHelper helper) {
        var distribution = RarityDistribution.of(List.of(
                new Rarity(RarityLevel.COMMON, 0.5F, false, "common"),
                new Rarity(RarityLevel.UNCOMMON, 0.25F, false, "uncommon")
        ));
        helper.assertTrue(distribution.select(CardType.CHICKEN, null, new SequenceRandom(0.4F, 0.3F))
                        .filter(rarity -> rarity == RarityLevel.COMMON).isPresent(),
                "Common must represent common success followed by uncommon failure");
        helper.assertTrue(distribution.select(CardType.CHICKEN, null, new SequenceRandom(0.4F, 0.2F))
                        .filter(rarity -> rarity == RarityLevel.UNCOMMON).isPresent(),
                "Uncommon success must replace an earlier common success");
        helper.assertTrue(distribution.select(CardType.CHICKEN, null, new SequenceRandom(0.7F, 0.3F)).isEmpty(),
                "No-card must represent both independent rolls failing");
        helper.succeed();
    }

    @GameTest
    public void malformedRarityDistributionsFailClosed(GameTestHelper helper) {
        assertIllegalArgument(helper, () -> RarityDistribution.of(List.of()), "empty distributions");
        assertIllegalArgument(helper, () -> RarityDistribution.of(List.of(
                new Rarity(RarityLevel.COMMON, 0.5F, false, "common"),
                new Rarity(RarityLevel.RARE, 0.1F, false, "rare")
        )), "non-contiguous rarities");
        assertIllegalArgument(helper, () -> RarityDistribution.of(List.of(
                new Rarity(RarityLevel.COMMON, 0.2F, false, "common"),
                new Rarity(RarityLevel.UNCOMMON, 0.3F, false, "uncommon")
        )), "increasing probabilities");
        assertIllegalArgument(helper, () -> RarityDistribution.of(List.of(
                new Rarity(RarityLevel.COMMON, Float.NaN, false, "common")
        )), "non-finite probabilities");
        helper.succeed();
    }

    @GameTest
    public void zeroProbabilityDisablesOnlyThatTier(GameTestHelper helper) {
        var distribution = RarityDistribution.of(List.of(
                new Rarity(RarityLevel.RARE, 0.0F, false, "rare"),
                new Rarity(RarityLevel.EPIC, 0.5F, false, "epic")
        ));
        helper.assertTrue(distribution.select(CardType.SQUID, null, new SequenceRandom(0.0F, 0.9F)).isEmpty(),
                "A zero-probability tier must never be rolled");
        assertIllegalArgument(helper, () -> RarityDistribution.of(List.of(
                new Rarity(RarityLevel.COMMON, -0.1F, false, "common")
        )), "negative probabilities");
        helper.succeed();
    }

    @GameTest
    public void dropRatesConfigUsesDefaultsAndAppliesValidOverrides(GameTestHelper helper) {
        var empty = new JsonObject();
        var defaults = DropRateSettings.CODEC.parse(JsonOps.INSTANCE, empty).getOrThrow();
        helper.assertTrue(empty.isEmpty() && defaults.distributions().isEmpty(),
                "An empty config must inherit defaults without mutating its input");
        var document = DropRateSettings.CODEC.encodeStart(JsonOps.INSTANCE, DropRateSettings.defaults())
                .getOrThrow().getAsJsonObject();
        var dropRates = document.getAsJsonObject("drop_rates");
        for (var cardType : CardType.values()) {
            var cardRates = dropRates.getAsJsonObject(cardType.getSerializedName());
            helper.assertTrue(cardRates != null && cardRates.size() == cardType.definition().rarityDistribution().rarities().size(),
                    cardType + " must be written with every supported rarity");
        }

        dropRates.getAsJsonObject("shulker").addProperty("legendary", 0.01);
        var overridden = DropRateSettings.CODEC.parse(JsonOps.INSTANCE, document).getOrThrow();
        var shulker = overridden.distributions().get(CardType.SHULKER);
        helper.assertTrue(shulker != null
                        && shulker.rarities().getLast().probability() == 0.01F
                        && shulker.rarities().getLast().description().equals(CardType.SHULKER.definition().rarityDistribution().rarities().getLast().description()),
                "A valid override must change only the configured probability");
        helper.succeed();
    }

    @GameTest
    public void dropRatesConfigRejectsInvalidEntries(GameTestHelper helper) {
        var root = DropRateSettings.CODEC.encodeStart(JsonOps.INSTANCE, DropRateSettings.defaults())
                .getOrThrow().getAsJsonObject();
        var dropRates = root.getAsJsonObject("drop_rates");
        dropRates.getAsJsonObject("shulker").addProperty("legendary", 0.9);
        dropRates.getAsJsonObject("endermite").addProperty("common", "lots");
        dropRates.getAsJsonObject("cow").addProperty("mythic", 0.5);
        dropRates.add("dragonfly", new JsonObject());

        var result = DropRateSettings.CODEC.parse(JsonOps.INSTANCE, root);
        helper.assertTrue(result.result().isEmpty() && result.error().isPresent(),
                "Invalid entries must reject the complete configuration");
        var message = result.error().orElseThrow().message();
        helper.assertTrue(message.contains("shulker") && message.contains("endermite")
                        && message.contains("mythic") && message.contains("dragonfly"),
                "Diagnostics must identify every invalid entry, got " + message);
        helper.succeed();
    }

    @GameTest
    public void equipmentEnforcesCapacityIdentityAndMutexRules(GameTestHelper helper) {
        var fiveCards = List.of(
                card(CardType.COW), card(CardType.SQUID), card(CardType.CHICKEN),
                card(CardType.BAT), card(CardType.HORSE)
        );
        helper.assertTrue(Equipment.create(fiveCards).result().isPresent(),
                "Five distinct compatible cards must be accepted");

        var sixCards = new ArrayList<>(fiveCards);
        sixCards.add(card(CardType.TURTLE));
        helper.assertTrue(Equipment.create(sixCards).error().isPresent(),
                "A sixth equipped card must be rejected");
        helper.assertTrue(Equipment.create(List.of(card(CardType.COW), card(CardType.COW))).error().isPresent(),
                "A card type must not be equipped twice");
        helper.assertTrue(Equipment.create(List.of(
                card(CardType.ELDER_WEREWOLF), card(CardType.ALPHA_WEREWOLF)
        )).error().isPresent(), "Mutex-group peers must not be equipped together");
        helper.succeed();
    }

    @GameTest
    public void equipmentReplacementAndDowngradeAreAtomic(GameTestHelper helper) {
        var commonChicken = new Card(CardType.CHICKEN, RarityLevel.COMMON);
        var rareChicken = new Card(CardType.CHICKEN, RarityLevel.RARE);
        var equipment = Equipment.create(List.of(commonChicken)).result().orElseThrow();

        var replaced = equipment.equipOrReplace(rareChicken).result().orElseThrow();
        helper.assertTrue(replaced.cards().equals(List.of(rareChicken)),
                "Equipping another rarity must replace the existing card type");
        helper.assertTrue(replaced.equipOrReplace(rareChicken).error().isPresent(),
                "Equipping the exact active card must be rejected without mutation");

        var downgraded = replaced.downgrade(rareChicken).result().orElseThrow();
        helper.assertTrue(downgraded.contains(CardType.CHICKEN, RarityLevel.UNCOMMON),
                "Downgrade must move to the preceding supported rarity");
        var removed = Equipment.create(List.of(commonChicken)).result().orElseThrow()
                .downgrade(commonChicken).result().orElseThrow();
        helper.assertTrue(removed.size() == 0,
                "Downgrading a minimum-rarity card must remove it");
        helper.succeed();
    }

    @GameTest
    public void persistedEquipmentRepairIsStableAndComplete(GameTestHelper helper) {
        var overCapacity = new EnumMap<CardType, RarityLevel>(CardType.class);
        overCapacity.put(CardType.COW, CardType.COW.minRarityLevel());
        overCapacity.put(CardType.SQUID, CardType.SQUID.minRarityLevel());
        overCapacity.put(CardType.CHICKEN, CardType.CHICKEN.minRarityLevel());
        overCapacity.put(CardType.BAT, CardType.BAT.minRarityLevel());
        overCapacity.put(CardType.HORSE, CardType.HORSE.minRarityLevel());
        overCapacity.put(CardType.TURTLE, CardType.TURTLE.minRarityLevel());
        overCapacity.put(CardType.NETHER, RarityLevel.COMMON);
        var repair = Equipment.repair(overCapacity);

        helper.assertTrue(repair.repaired().size() == Equipment.MAX_CARDS,
                "Repair must keep at most five valid cards in stable CardType order");
        helper.assertTrue(repair.discarded().keySet().equals(java.util.Set.of(CardType.TURTLE, CardType.NETHER)),
                "Repair must report both capacity overflow and unsupported rarities");

        var mutexRepair = Equipment.repair(Map.of(
                CardType.ELDER_WEREWOLF, RarityLevel.COMMON,
                CardType.ALPHA_WEREWOLF, RarityLevel.COMMON
        ));
        helper.assertTrue(mutexRepair.repaired().containsType(CardType.ELDER_WEREWOLF)
                        && mutexRepair.discarded().containsKey(CardType.ALPHA_WEREWOLF),
                "Repair must deterministically keep the first CardType in a mutex group");
        helper.succeed();
    }

    @GameTest
    public void playerStorageCodecRoundTripsEquipment(GameTestHelper helper) {
        var playerId = UUID.randomUUID();
        var original = new Storage(Map.of(playerId, new PlayerData(Map.of(
                CardType.BEE, RarityLevel.EPIC,
                CardType.CREEPER, RarityLevel.RARE
        ))));
        JsonElement encoded = Storage.CODEC.encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow(error -> new AssertionError("Could not encode storage: " + error));
        var decoded = Storage.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(error -> new AssertionError("Could not decode storage: " + error));

        helper.assertTrue(decoded.getPlayerDataMap().get(playerId).equippedCards().equals(
                        original.getPlayerDataMap().get(playerId).equippedCards()),
                "Saved equipment must survive a codec round-trip");
        helper.succeed();
    }

    private static Card card(CardType type) {
        return new Card(type, type.minRarityLevel());
    }

    private static void assertIllegalArgument(GameTestHelper helper, Runnable action, String subject) {
        try {
            action.run();
            helper.fail("Expected " + subject + " to be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected: the public constructor boundary rejects malformed configuration.
        }
    }

    private static final class SequenceRandom extends Random {
        private static final long serialVersionUID = 1L;
        private final float[] values;
        private int index;

        private SequenceRandom(float... values) {
            this.values = values;
        }

        @Override
        public float nextFloat() {
            return values[index++];
        }
    }
}
