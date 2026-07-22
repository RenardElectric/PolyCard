package polycube.polycard.events;

import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.zombie.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import polycube.polycard.card.CardType;
import polycube.polycard.events.callBacks.BreedEventCallback;
import polycube.polycard.events.callBacks.EntitySummonedEventCallback;
import polycube.polycard.events.callBacks.PlayerKillEventCallback;
import polycube.polycard.events.callBacks.TameEventCallback;
import polycube.polycard.utils.CardHelpers;
import polycube.polycard.utils.Helpers;

import java.util.Optional;

import static net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL;

public class CardLootEvents extends EventHandler implements BreedEventCallback, PlayerKillEventCallback, EntitySummonedEventCallback, TameEventCallback, BlockEvents.UseItemOnCallback {
    @Override
    public void onBreed(ServerPlayer player, Animal parent, Animal partner, Optional<AgeableMob> child) {
        switch (parent) {
            case Cow _ -> CardHelpers.receiveCard(player, CardType.COW);
            case Chicken _ -> CardHelpers.receiveCard(player, CardType.CHICKEN);
            case Turtle _ -> CardHelpers.receiveCard(player, CardType.TURTLE);
            default -> {
            }
        }
    }

    @Override
    public void onSummon(ServerPlayer player, Entity entity) {
        switch (entity) {
            case IronGolem _ -> CardHelpers.receiveCard(player, CardType.IRON_GOLEM);
            case WitherBoss _ -> CardHelpers.receiveCard(player, CardType.WITHER);
            case EnderDragon _ -> CardHelpers.receiveCard(player, CardType.ENDER_DRAGON);
            case CopperGolem _, SnowGolem _ -> {
            }
            default -> {
            }
        }
    }

    @Override
    public void onPlayerKill(ServerPlayer player, Entity entity, DamageSource killingBlow) {
        switch (entity) {
            case ZombieVillager _, Husk _, Drowned _ -> {
            }
            case EnderMan _ -> CardHelpers.receiveCard(player, CardType.ENDERMAN);
            case Squid _ -> CardHelpers.receiveCard(player, CardType.SQUID);
            case Piglin _ -> CardHelpers.receiveCard(player, CardType.PIGLIN);
            case ZombifiedPiglin _ -> CardHelpers.receiveCard(player, CardType.ZOMBIFIED_PIGLIN);
            case Zombie _ -> CardHelpers.receiveCard(player, CardType.ZOMBIE);
            case Bat _ -> CardHelpers.receiveCard(player, CardType.BAT);
            case Creeper _ -> CardHelpers.receiveCard(player, CardType.CREEPER);
            default -> {
            }
        }
    }

    @Override
    public void onTame(ServerPlayer player, Animal animal) {
        switch (animal) {
            case Horse _ -> CardHelpers.receiveCard(player, CardType.HORSE);
            case Wolf _ -> CardHelpers.receiveCard(player, CardType.WOLF);
            default -> {
            }
        }
    }

    @Override
    public @Nullable InteractionResult useItemOn(
            ItemStack itemStack, BlockState blockState,
            Level level, BlockPos blockPos, Player player,
            InteractionHand interactionHand, BlockHitResult blockHitResult
    ) {
        if (player.isSpectator()
                || !(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return null;
        }

        if ((blockState.is(Blocks.BEEHIVE) || blockState.is(Blocks.BEE_NEST))
                && blockState.getValue(HONEY_LEVEL) >= 5
                && itemStack.is(Items.GLASS_BOTTLE)) {
            Helpers.debug("{} successfully collected honey; rolling for a Bee card.", serverPlayer.getName().getString());
            CardHelpers.receiveCard(serverPlayer, CardType.BEE);
        }

        return null;
    }
}
