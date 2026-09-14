package polycube.polycard.gametest;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.events.CardLootEvents;
import polycube.polycard.events.callBacks.CardProbabilityOverrideCallback;
import polycube.polycard.utils.LootHelpers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL;

public final class AcquisitionGameTests {
    private static final Set<UUID> FORCED_DROPS = ConcurrentHashMap.newKeySet();

    static {
        CardProbabilityOverrideCallback.EVENT.register((player, cardType, rarity, original) -> {
            if (player != null && FORCED_DROPS.contains(player.getUUID())) {
                return cardType == CardType.RANDOM ? 0.0F : 1.0F;
            }
            return original;
        });
    }

    @GameTest
    public void breedingAwardsTheMatchingPassiveCard(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        var events = new CardLootEvents();
        forceDrops(player, () -> {
            assertAward(helper, player, CardType.COW, () -> events.onBreed(
                    player,
                    helper.spawn(EntityTypes.COW, new BlockPos(1, 2, 2)),
                    helper.spawn(EntityTypes.COW, new BlockPos(2, 2, 2)),
                    Optional.<AgeableMob>empty()));
            assertAward(helper, player, CardType.CHICKEN, () -> events.onBreed(
                    player,
                    helper.spawn(EntityTypes.CHICKEN, new BlockPos(1, 2, 2)),
                    helper.spawn(EntityTypes.CHICKEN, new BlockPos(2, 2, 2)),
                    Optional.<AgeableMob>empty()));
            assertAward(helper, player, CardType.TURTLE, () -> events.onBreed(
                    player,
                    helper.spawn(EntityTypes.TURTLE, new BlockPos(1, 2, 2)),
                    helper.spawn(EntityTypes.TURTLE, new BlockPos(2, 2, 2)),
                    Optional.<AgeableMob>empty()));
        });
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void tamingAndSummoningAwardTheMatchingCards(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        var events = new CardLootEvents();
        forceDrops(player, () -> {
            assertAward(helper, player, CardType.HORSE, () -> events.onTame(
                    player, helper.spawn(EntityTypes.HORSE, new BlockPos(1, 2, 2))));
            assertAward(helper, player, CardType.WOLF, () -> events.onTame(
                    player, helper.spawn(EntityTypes.WOLF, new BlockPos(1, 2, 2))));
            assertAward(helper, player, CardType.IRON_GOLEM, () -> events.onSummon(
                    player, create(helper, EntityTypes.IRON_GOLEM)));
            assertAward(helper, player, CardType.WITHER, () -> events.onSummon(
                    player, create(helper, EntityTypes.WITHER)));
            assertAward(helper, player, CardType.ENDER_DRAGON, () -> events.onSummon(
                    player, create(helper, EntityTypes.ENDER_DRAGON)));
        });
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void playerKillsAwardExactMobCardsAndExcludeZombieVariants(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        var events = new CardLootEvents();
        var expected = new LinkedHashMap<EntityType<?>, CardType>();
        expected.put(EntityTypes.ENDERMAN, CardType.ENDERMAN);
        expected.put(EntityTypes.SQUID, CardType.SQUID);
        expected.put(EntityTypes.PIGLIN, CardType.PIGLIN);
        expected.put(EntityTypes.ZOMBIFIED_PIGLIN, CardType.ZOMBIFIED_PIGLIN);
        expected.put(EntityTypes.ZOMBIE, CardType.ZOMBIE);
        expected.put(EntityTypes.BAT, CardType.BAT);
        expected.put(EntityTypes.CREEPER, CardType.CREEPER);
        expected.put(EntityTypes.PHANTOM, CardType.PHANTOM);

        forceDrops(player, () -> {
            for (var entry : expected.entrySet()) {
                assertAward(helper, player, entry.getValue(), () -> events.onPlayerKill(
                        player, create(helper, entry.getKey()), player.damageSources().generic()));
            }
            for (var excluded : List.of(EntityTypes.ZOMBIE_VILLAGER, EntityTypes.HUSK, EntityTypes.DROWNED)) {
                player.getInventory().clearContent();
                events.onPlayerKill(player, create(helper, excluded), player.damageSources().generic());
                helper.assertTrue(cards(player).isEmpty(),
                        excluded + " must not count as the base Zombie acquisition");
            }
        });
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void honeyCropsAndNaturalBlocksAwardMatchingCards(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        var events = new CardLootEvents();
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);

        forceDrops(player, () -> {
            assertAward(helper, player, CardType.BEE, () -> events.useItemOn(
                    new ItemStack(Items.GLASS_BOTTLE),
                    Blocks.BEEHIVE.defaultBlockState().setValue(HONEY_LEVEL, 5),
                    helper.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit));

            var matureWheat = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE);
            assertAward(helper, player, CardType.FARMER, () -> events.afterBlockBreak(
                    helper.getLevel(), player, pos, matureWheat, null));

            var ore = Blocks.DIAMOND_ORE.defaultBlockState();
            helper.assertTrue(ore.is(polycube.polycard.cardEffects.misc.MinerEffects.VEIN_MINABLE_BLOCKS),
                    "The GameTest data pack must load PolyCard's mining tags");
            assertAward(helper, player, CardType.MINER, () -> events.afterBlockBreak(
                    helper.getLevel(), player, pos, ore, null));
        });
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void goatRamsAndFullMoonWolfBitesAwardSpecialCards(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        var events = new CardLootEvents();
        forceDrops(player, () -> {
            var goat = helper.spawn(EntityTypes.GOAT, new BlockPos(2, 2, 2));
            assertAward(helper, player, CardType.GOAT, () -> events.afterEntityHurt(
                    player, helper.getLevel(), player.damageSources().noAggroMobAttack(goat), 3.0F));

            long previousTime = helper.getLevel().getDefaultClockTime();
            try {
                helper.setTime(13_000L);
                var wolf = helper.spawn(EntityTypes.WOLF, new BlockPos(2, 2, 2));
                player.getInventory().clearContent();
                events.afterEntityHurt(player, helper.getLevel(), player.damageSources().mobAttack(wolf), 3.0F);
                var awarded = cards(player);
                helper.assertTrue(awarded.size() == 1
                                && (awarded.getFirst().cardType() == CardType.ALPHA_WEREWOLF
                                || awarded.getFirst().cardType() == CardType.ELDER_WEREWOLF),
                        "A wild-wolf bite during a full-moon night must award one Werewolf family");
            } finally {
                helper.setTime(previousTime);
            }
        });
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void structureLootMappingsInjectTheExpectedPools(GameTestHelper helper) {
        var events = new CardLootEvents();
        helper.assertTrue(injectedPoolCount(helper, events, BuiltInLootTables.WOODLAND_MANSION) == 2,
                "Woodland mansion must receive Totem and general Lucky pools");
        helper.assertTrue(injectedPoolCount(helper, events, BuiltInLootTables.BURIED_TREASURE) == 2,
                "Buried treasure must receive Inventory and general Lucky pools");
        helper.assertTrue(injectedPoolCount(helper, events, BuiltInLootTables.BASTION_TREASURE) == 2,
                "Bastion treasure must receive Nether and general Lucky pools");
        helper.assertTrue(injectedPoolCount(helper, events, BuiltInLootTables.STRONGHOLD_LIBRARY) == 2,
                "Stronghold library must receive Spectator and general Lucky pools");
        helper.assertTrue(LootHelpers.lootPoolFromCardType(CardType.TOTEM).build().entries.size()
                        == CardType.TOTEM.getRarityDistribution().weightedOutcomes().size(),
                "A generated card loot pool must contain every weighted rarity and no-card outcome");
        helper.succeed();
    }

    private static int injectedPoolCount(
            GameTestHelper helper, CardLootEvents events,
            net.minecraft.resources.ResourceKey<LootTable> key
    ) {
        var builder = LootTable.lootTable();
        events.modifyLootTable(key, builder, LootTableSource.VANILLA, helper.getLevel().registryAccess());
        var ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        JsonElement encoded = LootTable.DIRECT_CODEC.encodeStart(ops, builder.build())
                .getOrThrow(error -> new AssertionError("Could not encode test loot table: " + error));
        return encoded.getAsJsonObject().getAsJsonArray("pools").size();
    }

    private static void forceDrops(net.minecraft.server.level.ServerPlayer player, Runnable action) {
        FORCED_DROPS.add(player.getUUID());
        try {
            action.run();
        } finally {
            FORCED_DROPS.remove(player.getUUID());
        }
    }

    private static void assertAward(
            GameTestHelper helper, net.minecraft.server.level.ServerPlayer player,
            CardType expectedType, Runnable trigger
    ) {
        player.getInventory().clearContent();
        trigger.run();
        var awarded = cards(player);
        helper.assertTrue(awarded.size() == 1 && awarded.getFirst().cardType() == expectedType,
                "Expected exactly one " + expectedType + " card, got " + awarded);
    }

    private static List<Card> cards(net.minecraft.server.level.ServerPlayer player) {
        var cards = new ArrayList<Card>();
        for (var stack : player.getInventory()) {
            Card.getCard(stack).ifPresent(cards::add);
        }
        return cards;
    }

    private static Entity create(GameTestHelper helper, EntityType<?> type) {
        Entity entity = type.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        if (entity == null) throw new AssertionError("Could not create " + type);
        return entity;
    }
}
