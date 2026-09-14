package polycube.polycard.gametest;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ServerLevelData;
import org.apache.commons.lang3.mutable.MutableFloat;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.misc.FarmerEffects;
import polycube.polycard.cardEffects.misc.LootEffects;
import polycube.polycard.cardEffects.misc.MinerEffects;
import polycube.polycard.events.callBacks.AllowPhantomSpawnEventCallback;
import polycube.polycard.events.callBacks.CardProbabilityOverrideCallback;
import polycube.polycard.events.callBacks.EntityAfterHurtEventCallback;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.ExplosionKnockbackEventCallback;
import polycube.polycard.events.callBacks.GetBedRuleEventCallback;
import polycube.polycard.events.callBacks.IsTargetedEventCallback;
import polycube.polycard.events.callBacks.ItemConsumedEventCallback;
import polycube.polycard.events.callBacks.KeepInventoryEventCallback;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;

import java.util.List;

public final class CardEffectGameTests {
    private static final IsTargetedEventCallback.TargetingConditionsData COMBAT_TARGETING =
            new IsTargetedEventCallback.TargetingConditionsData(true, 32.0D, true, false, null, true);

    @GameTest
    public void deterministicConsumableEffectsRespectEquippedRarity(GameTestHelper helper) {
        var beePlayer = GameTestSupport.player(helper);
        GameTestSupport.equip(helper, beePlayer, List.of(new Card(CardType.BEE, RarityLevel.LEGENDARY)));
        ItemConsumedEventCallback.EVENT.invoker().onItemConsumed(beePlayer, new ItemStack(Items.HONEY_BOTTLE));
        helper.assertTrue(beePlayer.hasEffect(MobEffects.SPEED)
                        && beePlayer.hasEffect(MobEffects.REGENERATION)
                        && beePlayer.hasEffect(MobEffects.HEALTH_BOOST),
                "Legendary Bee must apply every honey-bottle buff tier");

        var zombiePlayer = GameTestSupport.player(helper);
        zombiePlayer.getFoodData().setFoodLevel(10);
        GameTestSupport.equip(helper, zombiePlayer, List.of(new Card(CardType.ZOMBIE, RarityLevel.EPIC)));
        ItemConsumedEventCallback.EVENT.invoker().onItemConsumed(zombiePlayer, new ItemStack(Items.ROTTEN_FLESH));
        helper.assertTrue(zombiePlayer.getFoodData().getFoodLevel() == 12
                        && zombiePlayer.hasEffect(MobEffects.STRENGTH)
                        && zombiePlayer.hasEffect(MobEffects.REGENERATION),
                "Epic Zombie must improve rotten flesh and apply its unlocked buffs");

        var piglinPlayer = GameTestSupport.player(helper);
        GameTestSupport.equip(helper, piglinPlayer, List.of(new Card(CardType.PIGLIN, RarityLevel.RARE)));
        ItemConsumedEventCallback.EVENT.invoker().onItemConsumed(piglinPlayer, new ItemStack(Items.GOLDEN_CARROT));
        helper.assertTrue(piglinPlayer.getActiveEffects().stream()
                        .anyMatch(effect -> polycube.polycard.cardEffects.neutral.PiglinEffects.BUFFS.contains(effect.getEffect())),
                "Rare Piglin must grant one configured buff after golden food");

        var cowPlayer = GameTestSupport.player(helper);
        cowPlayer.setHealth(2.0F);
        GameTestSupport.equip(helper, cowPlayer, List.of(new Card(CardType.COW, RarityLevel.LEGENDARY)));
        ItemConsumedEventCallback.EVENT.invoker().onItemConsumed(cowPlayer, new ItemStack(Items.MILK_BUCKET));
        helper.assertTrue(cowPlayer.getHealth() == 18.0F,
                "Legendary Cow must heal sixteen health points after milk");

        beePlayer.discard();
        zombiePlayer.discard();
        piglinPlayer.discard();
        cowPlayer.discard();
        helper.succeed();
    }

    @GameTest
    public void persistentTickEffectsApplyOnlyWhenTheirConditionsHold(GameTestHelper helper) {
        var batPlayer = GameTestSupport.player(helper);
        GameTestSupport.equip(helper, batPlayer, List.of(new Card(CardType.BAT, RarityLevel.RARE)));
        PlayerTickEventCallback.EVENT.invoker().onPlayerTick(helper.getLevel().getServer(), batPlayer);
        helper.assertTrue(batPlayer.hasEffect(MobEffects.NIGHT_VISION),
                "Rare Bat must grant night vision");

        var chickenPlayer = GameTestSupport.player(helper);
        GameTestSupport.equip(helper, chickenPlayer, List.of(new Card(CardType.CHICKEN, RarityLevel.LEGENDARY)));
        chickenPlayer.fallDistance = 3.0F;
        chickenPlayer.setPose(Pose.CROUCHING);
        PlayerTickEventCallback.EVENT.invoker().onPlayerTick(helper.getLevel().getServer(), chickenPlayer);
        helper.assertTrue(chickenPlayer.hasEffect(MobEffects.SLOW_FALLING),
                "Legendary Chicken must grant slow falling while crouching in a fall");

        var netherPlayer = GameTestSupport.player(helper);
        GameTestSupport.equip(helper, netherPlayer, List.of(new Card(CardType.NETHER, RarityLevel.LEGENDARY)));
        PlayerTickEventCallback.EVENT.invoker().onPlayerTick(helper.getLevel().getServer(), netherPlayer);
        helper.assertTrue(netherPlayer.hasEffect(MobEffects.FIRE_RESISTANCE),
                "Legendary Nether must grant fire resistance");

        var originalRule = new BedRule(BedRule.Rule.ALWAYS, BedRule.Rule.ALWAYS, true, true, java.util.Optional.empty());
        var netherRule = GetBedRuleEventCallback.EVENT.invoker().getBedRule(netherPlayer, originalRule);
        helper.assertTrue(netherRule.canSleep() == BedRule.Rule.NEVER
                        && netherRule.canSetSpawn() == BedRule.Rule.NEVER,
                "Legendary Nether must forbid Overworld sleeping and respawn placement");

        batPlayer.discard();
        chickenPlayer.discard();
        netherPlayer.discard();
        helper.succeed();
    }

    @GameTest
    public void hostileMobsRespectCardTargetingImmunities(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        var level = helper.getLevel();

        GameTestSupport.equip(helper, player, List.of(new Card(CardType.CREEPER, RarityLevel.LEGENDARY)));
        helper.assertTrue(IsTargetedEventCallback.EVENT.invoker().onTargeted(
                        level, helper.spawn(EntityTypes.CREEPER, new BlockPos(1, 2, 2)), player, COMBAT_TARGETING
                ) == InteractionResult.FAIL,
                "Legendary Creeper must stop Creepers targeting the player");

        GameTestSupport.equip(helper, player, List.of(new Card(CardType.PIGLIN, RarityLevel.EPIC)));
        helper.assertTrue(IsTargetedEventCallback.EVENT.invoker().onTargeted(
                        level, helper.spawn(EntityTypes.PIGLIN, new BlockPos(1, 2, 2)), player, COMBAT_TARGETING
                ) == InteractionResult.FAIL,
                "Uncommon Piglin or better must stop Piglins targeting the player");
        helper.assertTrue(IsTargetedEventCallback.EVENT.invoker().onTargeted(
                        level, helper.spawn(EntityTypes.PIGLIN_BRUTE, new BlockPos(1, 2, 2)), player, COMBAT_TARGETING
                ) == InteractionResult.FAIL,
                "Epic Piglin must also stop Piglin Brutes targeting the player");

        GameTestSupport.equip(helper, player, List.of(new Card(CardType.ZOMBIE, RarityLevel.LEGENDARY)));
        helper.assertTrue(IsTargetedEventCallback.EVENT.invoker().onTargeted(
                        level, helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 2)), player, COMBAT_TARGETING
                ) == InteractionResult.FAIL,
                "Legendary Zombie must stop undead variants targeting the player");

        GameTestSupport.equip(helper, player, List.of(new Card(CardType.ZOMBIFIED_PIGLIN, RarityLevel.EPIC)));
        helper.assertTrue(IsTargetedEventCallback.EVENT.invoker().onTargeted(
                        level, helper.spawn(EntityTypes.ZOMBIFIED_PIGLIN, new BlockPos(1, 2, 2)), player, COMBAT_TARGETING
                ) == InteractionResult.FAIL,
                "Epic Zombified Piglin must stop its matching mob targeting the player");

        GameTestSupport.equip(helper, player, List.of(new Card(CardType.PHANTOM, RarityLevel.LEGENDARY)));
        helper.assertTrue(IsTargetedEventCallback.EVENT.invoker().onTargeted(
                        level, helper.spawn(EntityTypes.PHANTOM, new BlockPos(1, 3, 2)), player, COMBAT_TARGETING
                ) == InteractionResult.FAIL,
                "Epic Phantom or better must stop Phantoms targeting the player");
        helper.assertTrue(!AllowPhantomSpawnEventCallback.EVENT.invoker().allowPhantomSpawn(player, level),
                "Legendary Phantom must prevent Phantom spawns");

        player.discard();
        helper.succeed();
    }

    @GameTest
    public void damageFiltersAndExplosionReductionUseAcceptedDamageSemantics(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        var level = helper.getLevel();

        GameTestSupport.equip(helper, player, List.of(new Card(CardType.CREEPER, RarityLevel.RARE)));
        var explosionDamage = new MutableFloat(10.0F);
        helper.assertTrue(EntityHurtEventCallback.EVENT.invoker().onEntityHurt(
                        player, level, player.damageSources().explosion(null, null), explosionDamage
                ) == InteractionResult.PASS && explosionDamage.floatValue() == 5.0F,
                "Uncommon Creeper or better must halve explosion damage without cancelling the hit");
        helper.assertTrue(ExplosionKnockbackEventCallback.EVENT.invoker().onExplosionKnockback(player, 10.0F) == 5.0F,
                "Rare Creeper must halve explosion knockback");

        GameTestSupport.equip(helper, player, List.of(new Card(CardType.ENDERMAN, RarityLevel.RARE)));
        helper.assertTrue(EntityHurtEventCallback.EVENT.invoker().onEntityHurt(
                        player, level, player.damageSources().enderPearl(), new MutableFloat(5.0F)
                ) == InteractionResult.FAIL,
                "Rare Enderman must cancel ender-pearl damage");

        GameTestSupport.equip(helper, player, List.of(new Card(CardType.WITHER, RarityLevel.UNCOMMON)));
        helper.assertTrue(EntityHurtEventCallback.EVENT.invoker().onEntityHurt(
                        player, level, player.damageSources().wither(), new MutableFloat(5.0F)
                ) == InteractionResult.FAIL,
                "Uncommon Wither must cancel Wither damage");

        GameTestSupport.equip(helper, player, List.of(new Card(CardType.ENDER_DRAGON, RarityLevel.EPIC)));
        helper.assertTrue(EntityHurtEventCallback.EVENT.invoker().onEntityHurt(
                        player, level, player.damageSources().flyIntoWall(), new MutableFloat(5.0F)
                ) == InteractionResult.FAIL,
                "Epic Ender Dragon must cancel kinetic wall damage");
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void spectatorCardBlocksIncomingAndOutgoingDamage(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        var target = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 2, 2));
        GameTestSupport.equip(helper, player, List.of(new Card(CardType.SPECTATOR, RarityLevel.LEGENDARY)));

        helper.assertTrue(EntityHurtEventCallback.EVENT.invoker().onEntityHurt(
                        player, helper.getLevel(), player.damageSources().generic(), new MutableFloat(5.0F)
                ) == InteractionResult.FAIL,
                "Spectator must cancel incoming damage");
        helper.assertTrue(EntityHurtEventCallback.EVENT.invoker().onEntityHurt(
                        target, helper.getLevel(), player.damageSources().playerAttack(player), new MutableFloat(5.0F)
                ) == InteractionResult.FAIL,
                "Spectator must cancel outgoing damage");
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void lowHealthTurtleAndSprintingGoatApplyCombatEffects(GameTestHelper helper) {
        var turtlePlayer = GameTestSupport.player(helper);
        turtlePlayer.setHealth(4.0F);
        GameTestSupport.equip(helper, turtlePlayer, List.of(new Card(CardType.TURTLE, RarityLevel.LEGENDARY)));
        EntityAfterHurtEventCallback.EVENT.invoker().afterEntityHurt(
                turtlePlayer, helper.getLevel(), turtlePlayer.damageSources().generic(), 2.0F);
        helper.assertTrue(turtlePlayer.hasEffect(MobEffects.SLOWNESS)
                        && turtlePlayer.getEffect(MobEffects.RESISTANCE).getAmplifier() == 3,
                "Legendary Turtle must grant Turtle Master at low health");

        var goatPlayer = GameTestSupport.player(helper);
        var target = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 2, 2));
        GameTestSupport.equip(helper, goatPlayer, List.of(new Card(CardType.GOAT, RarityLevel.EPIC)));
        goatPlayer.setSprinting(true);
        target.setDeltaMovement(0.0D, 0.0D, 0.0D);
        EntityAfterHurtEventCallback.EVENT.invoker().afterEntityHurt(
                target, helper.getLevel(), goatPlayer.damageSources().playerAttack(goatPlayer), 2.0F);
        helper.assertTrue(target.getDeltaMovement().horizontalDistanceSqr() > 0.0D,
                "Epic Goat must add knockback to a direct sprinting melee hit");

        turtlePlayer.discard();
        goatPlayer.discard();
        helper.succeed();
    }

    @GameTest
    public void squidBlindnessUsesTheMatchingAttackerAndCardOwner(GameTestHelper helper) {
        var level = helper.getLevel();
        var defender = GameTestSupport.player(helper);
        var attacker = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 2, 2));
        GameTestSupport.equip(helper, defender, List.of(new Card(CardType.SQUID, RarityLevel.RARE)));
        forceNextLevelFloatBelow(level, 0.1F);
        EntityAfterHurtEventCallback.EVENT.invoker().afterEntityHurt(
                defender, level, defender.damageSources().mobAttack(attacker), 2.0F);
        helper.assertTrue(attacker.hasEffect(MobEffects.BLINDNESS),
                "Rare Squid must blind the living entity that attacked its owner when the chance succeeds");

        var sourcePlayer = GameTestSupport.player(helper);
        var target = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(3, 2, 2));
        GameTestSupport.equip(helper, sourcePlayer, List.of(new Card(CardType.SQUID, RarityLevel.EPIC)));
        forceNextLevelFloatBelow(level, 0.1F);
        EntityAfterHurtEventCallback.EVENT.invoker().afterEntityHurt(
                target, level, sourcePlayer.damageSources().playerAttack(sourcePlayer), 2.0F);
        helper.assertTrue(target.hasEffect(MobEffects.BLINDNESS),
                "Epic Squid must blind the entity hit by its owner when the chance succeeds");

        defender.discard();
        sourcePlayer.discard();
        helper.succeed();
    }

    @GameTest
    public void chickenHorseAndIronGolemUseTheirRuntimeConditions(GameTestHelper helper) {
        var level = helper.getLevel();

        var chickenPlayer = GameTestSupport.player(helper);
        GameTestSupport.equip(helper, chickenPlayer, List.of(new Card(CardType.CHICKEN, RarityLevel.UNCOMMON)));
        var egg = new ThrownEgg(level, chickenPlayer, new ItemStack(Items.EGG));
        var eggDamage = new MutableFloat(5.0F);
        EntityHurtEventCallback.EVENT.invoker().onEntityHurt(
                helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 2, 2)),
                level, chickenPlayer.damageSources().thrown(egg, chickenPlayer), eggDamage);
        helper.assertTrue(eggDamage.floatValue() == 1.0F,
                "Uncommon Chicken must make a player-thrown egg deal exactly one damage");

        var horsePlayer = GameTestSupport.player(helper);
        GameTestSupport.equip(helper, horsePlayer, List.of(new Card(CardType.HORSE, RarityLevel.EPIC)));
        Horse horse = helper.spawn(EntityTypes.HORSE, new BlockPos(2, 2, 2));
        horse.setTamed(true);
        horse.setOwner(horsePlayer);
        horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        helper.assertTrue(horsePlayer.startRiding(horse, true, false)
                        && horse.getControllingPassenger() == horsePlayer,
                "The Horse test player must control the saddled horse");
        var horseDamage = new MutableFloat(10.0F);
        EntityHurtEventCallback.EVENT.invoker().onEntityHurt(
                horse, level, horsePlayer.damageSources().generic(), horseDamage);
        PlayerTickEventCallback.EVENT.invoker().onPlayerTick(level.getServer(), horsePlayer);
        helper.assertTrue(horseDamage.floatValue() == 5.0F
                        && horse.hasEffect(MobEffects.JUMP_BOOST)
                        && horse.hasEffect(MobEffects.SPEED),
                "Epic Horse must halve ridden-horse damage and grant its riding buffs");

        var golemPlayer = GameTestSupport.player(helper);
        GameTestSupport.equip(helper, golemPlayer, List.of(new Card(CardType.IRON_GOLEM, RarityLevel.EPIC)));
        var golemTarget = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(3, 2, 2));
        golemTarget.setDeltaMovement(0.0D, 0.0D, 0.0D);
        EntityAfterHurtEventCallback.EVENT.invoker().afterEntityHurt(
                golemTarget, level, golemPlayer.damageSources().playerAttack(golemPlayer), 2.0F);
        helper.assertTrue(golemTarget.getDeltaMovement().horizontalDistanceSqr() > 0.0D,
                "Epic Iron Golem must knock back enemies hit with an empty hand");

        chickenPlayer.discard();
        horsePlayer.discard();
        golemPlayer.discard();
        helper.succeed();
    }

    @GameTest
    public void enderDragonGliderCapabilityFollowsEquipment(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        var chestplate = new ItemStack(Items.IRON_CHESTPLATE);
        player.setItemSlot(EquipmentSlot.CHEST, chestplate);

        GameTestSupport.equip(helper, player, List.of(new Card(CardType.ENDER_DRAGON, RarityLevel.LEGENDARY)));
        helper.assertTrue(chestplate.has(DataComponents.GLIDER),
                "Legendary Ender Dragon must add gliding to worn chest equipment");
        GameTestSupport.equip(helper, player, List.of());
        helper.assertTrue(!chestplate.has(DataComponents.GLIDER),
                "Unequipping Ender Dragon must remove only its owned glider component");
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void deathRelatedCardsPreserveTheirTransactionRules(GameTestHelper helper) {
        var inventoryPlayer = GameTestSupport.player(helper);
        var inventoryCard = new Card(CardType.INVENTORY, RarityLevel.LEGENDARY);
        GameTestSupport.equip(helper, inventoryPlayer, List.of(inventoryCard));
        helper.assertTrue(KeepInventoryEventCallback.EVENT.invoker().onKeepInventory(
                        inventoryPlayer, helper.getLevel(), false) == InteractionResult.SUCCESS,
                "Inventory must keep items when it is the player's only equipped card");
        GameTestSupport.equip(helper, inventoryPlayer, List.of(
                inventoryCard, new Card(CardType.CHICKEN, RarityLevel.COMMON)));
        helper.assertTrue(KeepInventoryEventCallback.EVENT.invoker().onKeepInventory(
                        inventoryPlayer, helper.getLevel(), false) == InteractionResult.PASS,
                "Inventory must not override keep-inventory when another card is equipped");

        var lifePlayer = GameTestSupport.player(helper);
        var life = new Card(CardType.LIFE, RarityLevel.RARE);
        GameTestSupport.equip(helper, lifePlayer, List.of(life));
        ServerLivingEntityEvents.AFTER_DEATH.invoker().afterDeath(lifePlayer, lifePlayer.damageSources().generic());
        helper.assertTrue(GameTestSupport.data(lifePlayer).hasCard(CardType.LIFE, RarityLevel.UNCOMMON),
                "Life must lose exactly one supported rarity after death");

        var totemPlayer = GameTestSupport.player(helper);
        var originalOffhand = new ItemStack(Items.DIAMOND);
        totemPlayer.setItemInHand(InteractionHand.OFF_HAND, originalOffhand);
        GameTestSupport.equip(helper, totemPlayer, List.of(new Card(CardType.TOTEM, RarityLevel.RARE)));
        ServerLivingEntityEvents.ALLOW_DEATH.invoker().allowDeath(
                totemPlayer, totemPlayer.damageSources().generic(), 100.0F);
        helper.assertTrue(totemPlayer.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
                        && GameTestSupport.data(totemPlayer).hasCard(CardType.TOTEM, RarityLevel.UNCOMMON),
                "Totem must downgrade before temporarily providing vanilla death protection");
        ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(
                totemPlayer, totemPlayer.damageSources().generic(), 100.0F, 20.0F, false);
        helper.assertTrue(totemPlayer.getOffhandItem() == originalOffhand,
                "Totem must restore the exact off-hand stack after damage resolves");

        inventoryPlayer.discard();
        lifePlayer.discard();
        totemPlayer.discard();
        helper.succeed();
    }

    @GameTest
    public void lootProbabilityMultiplierRequiresAndScalesWithLootCard(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        float original = 0.10F;
        float withoutCard = CardProbabilityOverrideCallback.EVENT.invoker().cardProbabilityOverride(
                player, CardType.BEE, RarityLevel.RARE, original);
        helper.assertTrue(withoutCard == original,
                "Card probabilities must remain unchanged without an equipped Loot card");

        GameTestSupport.equip(helper, player, List.of(new Card(CardType.LOOT, RarityLevel.LEGENDARY)));
        float withLegendary = CardProbabilityOverrideCallback.EVENT.invoker().cardProbabilityOverride(
                player, CardType.BEE, RarityLevel.RARE, original);
        float expected = (float) (original * Math.pow(LootEffects.PROBABILITY_MULTIPLIER, 5));
        helper.assertTrue(Math.abs(withLegendary - expected) < 0.000001F,
                "Legendary Loot must apply all five cumulative probability multipliers");
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void farmerHoeTransformsTheEightMatchingNeighbors(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_HOE));
        var center = new BlockPos(2, 1, 2);
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
            }
        }

        FarmerEffects.hoe3x3(
                helper.getLevel(), player, helper.absolutePos(center),
                Blocks.DIRT.defaultBlockState(), InteractionHand.MAIN_HAND);
        int farmland = 0;
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                if (helper.getBlockState(new BlockPos(x, 1, z)).is(Blocks.FARMLAND)) farmland++;
            }
        }
        helper.assertTrue(farmland == 8 && helper.getBlockState(center).is(Blocks.DIRT),
                "Farmer 3x3 hoeing must transform every matching neighbor but leave the clicked block to vanilla");
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void minerVeinMiningStopsAtSixteenConnectedBlocks(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        player.startUsingItem(InteractionHand.MAIN_HAND);
        for (int x = 1; x <= 3; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = 1; z <= 3; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        var broken = new int[1];
        MinerEffects.veinMine(
                helper.getLevel(), player, helper.absolutePos(new BlockPos(2, 2, 2)),
                state -> state.is(Blocks.STONE),
                (level, serverPlayer, pos, state) -> broken[0]++);

        helper.assertTrue(broken[0] == MinerEffects.VEIN_MINE_BLOCK_COUNT,
                "Vein mining must break exactly sixteen connected matching blocks at its cap");
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void wolfAndWerewolfAttributesTrackEquipment(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        double baseDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        GameTestSupport.equip(helper, player, List.of(new Card(CardType.ALPHA_WEREWOLF, RarityLevel.LEGENDARY)));
        helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) != baseDamage,
                "Equipping Alpha Werewolf must apply its moon-scaled attack modifier");
        GameTestSupport.equip(helper, player, List.of());
        helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == baseDamage,
                "Unequipping Alpha Werewolf must remove its attack modifier");

        var wolf = helper.spawn(EntityTypes.WOLF, new BlockPos(2, 2, 2));
        wolf.setOwner(player);
        GameTestSupport.equip(helper, player, List.of(new Card(CardType.WOLF, RarityLevel.RARE)));
        var levelData = (ServerLevelData) helper.getLevel().getLevelData();
        long previousGameTime = levelData.getGameTime();
        try {
            levelData.setGameTime(20L);
            ServerTickEvents.END_LEVEL_TICK.invoker().onEndTick(helper.getLevel());
            helper.assertTrue(wolf.hasEffect(MobEffects.RESISTANCE) && wolf.hasEffect(MobEffects.STRENGTH),
                    "Rare Wolf must refresh resistance and strength on an owned wolf");
        } finally {
            levelData.setGameTime(previousGameTime);
        }
        player.discard();
        helper.succeed();
    }

    private static void forceNextLevelFloatBelow(net.minecraft.server.level.ServerLevel level, float threshold) {
        long seed = 0L;
        do {
            level.getRandom().setSeed(seed++);
        } while (level.getRandom().nextFloat() >= threshold);
        level.getRandom().setSeed(seed - 1L);
    }
}
