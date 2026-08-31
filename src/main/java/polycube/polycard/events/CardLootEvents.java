package polycube.polycard.events;

import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.zombie.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import polycube.polycard.PolyCard;
import polycube.polycard.card.CardType;
import polycube.polycard.events.callBacks.*;
import polycube.polycard.utils.CardHelpers;
import polycube.polycard.utils.LootHelpers;

import java.util.Optional;

import static net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL;

public class CardLootEvents
        extends EventHandler
        implements BreedEventCallback, PlayerKillEventCallback, EntitySummonedEventCallback,
        TameEventCallback, BlockEvents.UseItemOnCallback, LootTableEvents.Modify,
        EntityAfterHurtEventCallback, PlayerBlockBreakEvents.After
{
    @Override
    public void onBreed(ServerPlayer player, Animal parent, Animal partner, Optional<AgeableMob> child) {
        switch (parent) {
            case Cow _ -> CardHelpers.receiveCard(player, CardType.COW);
            case Chicken _ -> CardHelpers.receiveCard(player, CardType.CHICKEN);
            case Turtle _ -> CardHelpers.receiveCard(player, CardType.TURTLE);
            default -> {}
        }
    }

    @Override
    public void onSummon(ServerPlayer player, Entity entity) {
        switch (entity) {
            case IronGolem _ -> CardHelpers.receiveCard(player, CardType.IRON_GOLEM);
            case WitherBoss _ -> CardHelpers.receiveCard(player, CardType.WITHER);
            case EnderDragon _ -> CardHelpers.receiveCard(player, CardType.ENDER_DRAGON);
            case CopperGolem _, SnowGolem _ -> {}
            default -> {}
        }
    }

    @Override
    public void onPlayerKill(ServerPlayer player, Entity entity, DamageSource killingBlow) {
        switch (entity) {
            case ZombieVillager _, Husk _, Drowned _ -> {}
            case EnderMan _ -> CardHelpers.receiveCard(player, CardType.ENDERMAN);
            case Squid _ -> CardHelpers.receiveCard(player, CardType.SQUID);
            case Piglin _ -> CardHelpers.receiveCard(player, CardType.PIGLIN);
            case ZombifiedPiglin _ -> CardHelpers.receiveCard(player, CardType.ZOMBIFIED_PIGLIN);
            case Zombie _ -> CardHelpers.receiveCard(player, CardType.ZOMBIE);
            case Bat _ -> CardHelpers.receiveCard(player, CardType.BAT);
            case Creeper _ -> CardHelpers.receiveCard(player, CardType.CREEPER);
            case Phantom _ -> CardHelpers.receiveCard(player, CardType.PHANTOM);
            default -> {}
        }
    }

    @Override
    public void onTame(ServerPlayer player, Animal animal) {
        switch (animal) {
            case Horse _ -> CardHelpers.receiveCard(player, CardType.HORSE);
            case Wolf _ -> CardHelpers.receiveCard(player, CardType.WOLF);
            default -> {}
        }
    }

    @Override
    public @Nullable InteractionResult useItemOn(
            ItemStack itemStack, BlockState blockState,
            Level level, BlockPos blockPos, Player player,
            InteractionHand interactionHand, BlockHitResult blockHitResult
    ) {
        if (player.isSpectator() || !(player instanceof ServerPlayer serverPlayer)) {
            return null;
        }

        if ((blockState.is(Blocks.BEEHIVE) || blockState.is(Blocks.BEE_NEST))
                && blockState.getValue(HONEY_LEVEL) >= 5
                && itemStack.is(Items.GLASS_BOTTLE)) {
            PolyCard.LOGGER.debug("{} successfully collected honey; rolling for a Bee card.", serverPlayer.getName().getString());
            CardHelpers.receiveCard(serverPlayer, CardType.BEE);
        }

        return null;
    }

    @Override
    public void modifyLootTable(ResourceKey<LootTable> key, LootTable.Builder tableBuilder, LootTableSource source, HolderLookup.Provider holder) {
        if (source.isBuiltin()) {
            var path = key.identifier().getPath();
            if (key.equals(BuiltInLootTables.WOODLAND_MANSION)) {
                tableBuilder.withPool(LootHelpers.lootPoolFromCardType(CardType.TOTEM));
            }
            if (key.equals(BuiltInLootTables.BASTION_BRIDGE) || key.equals(BuiltInLootTables.BASTION_HOGLIN_STABLE)
                    || key.equals(BuiltInLootTables.BASTION_OTHER) || key.equals(BuiltInLootTables.BASTION_TREASURE)
                    || key.equals(BuiltInLootTables.NETHER_BRIDGE)) {
                tableBuilder.withPool(LootHelpers.lootPoolFromCardType(CardType.NETHER));
            }
            if (BuiltInLootTables.all().contains(key) && (path.startsWith("chests/")
                    || path.startsWith("dispensers/") || path.startsWith("pots/")
                    || path.startsWith("archaeology/") || key.equals(BuiltInLootTables.SPAWNER_TRIAL_CHAMBER_CONSUMABLES)
                    || key.equals(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES) || key.equals(BuiltInLootTables.SPAWNER_TRIAL_ITEMS_TO_DROP_WHEN_OMINOUS))) {
                tableBuilder.withPool(LootHelpers.lootPoolFromCardType(CardType.LUCKY));
            }
            if (key.equals(BuiltInLootTables.BURIED_TREASURE)) {
                tableBuilder.withPool(LootHelpers.lootPoolFromCardType(CardType.INVENTORY));
            }
            if (key.equals(BuiltInLootTables.STRONGHOLD_CORRIDOR) || key.equals(BuiltInLootTables.STRONGHOLD_CROSSING) || key.equals(BuiltInLootTables.STRONGHOLD_LIBRARY)) {
                tableBuilder.withPool(LootHelpers.lootPoolFromCardType(CardType.SPECTATOR));
            }
            if (BuiltInLootTables.all().contains(key) && path.startsWith("chests/village")) {
                tableBuilder.withPool(LootHelpers.lootPoolFromCardType(CardType.LIFE));
            }
        }
    }

    @Override
    public void afterEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, float damageDealt) {
        if (entity instanceof ServerPlayer player) {
            if (source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)
                    && source.getEntity() instanceof Goat goat
                    && source.getDirectEntity() == goat) {
                CardHelpers.receiveCard(player, CardType.GOAT);
            }

            if (source.getEntity() instanceof LivingEntity attacker) {
                var time = level.getDefaultClockTime() % 24000;
                if (attacker instanceof Wolf wolf && !wolf.isTame()
                        &&  time >= 13000 && time < 23000
                        && level.environmentAttributes().getDimensionValue(EnvironmentAttributes.MOON_PHASE) == MoonPhase.FULL_MOON) {
                    if (player.getRandom().nextDouble() < 0.5)
                        CardHelpers.receiveCard(player, CardType.ALPHA_WEREWOLF);
                    else
                        CardHelpers.receiveCard(player, CardType.ELDER_WEREWOLF);
                }
            }
        }
    }

    @Override
    public void afterBlockBreak(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (state.getBlock() instanceof CropBlock block && block.isMaxAge(state)) {
                CardHelpers.receiveCard(serverPlayer, CardType.FARMER);
            }
        }
    }
}
