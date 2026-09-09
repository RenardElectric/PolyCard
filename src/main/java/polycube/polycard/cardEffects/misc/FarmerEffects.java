package polycube.polycard.cardEffects.misc;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.utils.BlockBreakHelpers;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class FarmerEffects extends CardEffects implements BlockEvents.UseItemOnCallback {
    @Override
    public @Nullable InteractionResult useItemOn(ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            if (itemStack.is(Items.COPPER_HOE)) {
                if (blockState.getBlock() instanceof CropBlock block && block.isMaxAge(blockState) && hasCardOrRarer(serverPlayer, RarityLevel.LEGENDARY)) {
                    BlockBreakHelpers.breakBlock(serverLevel, serverPlayer, blockPos, true, FarmerEffects::afterCropBreak);
                    MinerEffects.mine3x3(serverLevel, serverPlayer, blockPos, newState -> newState.is(block) && block.isMaxAge(newState), FarmerEffects::afterCropBreak, false);
                } else if (HoeItem.TILLABLES.containsKey(blockState.getBlock()) && hasCardOrRarer(serverPlayer, RarityLevel.EPIC)) {
                    hoe3x3(level, serverPlayer, blockPos, blockState, interactionHand);
                }
            }
        }
        return null;
    }

    public static void afterCropBreak(Level level, ServerPlayer player, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof CropBlock) {
            ItemStack itemStack = state.getCloneItemStack(level, pos, false);
            var blockHitResult = new BlockHitResult(player.position(), Direction.UP, pos, false);
            if (player.hasInfiniteMaterials()) {
                replantCrop(player, level, itemStack, blockHitResult);
            } else {
                for (ItemStack slotItem : player.getInventory()) {
                    if (slotItem.getItem() == itemStack.getItem()) {
                        replantCrop(player, level, slotItem, blockHitResult);
                        return;
                    }
                }
            }
        }
    }

    public static void replantCrop(ServerPlayer player, Level level, ItemStack itemStack, BlockHitResult hitResult) {
        //code from ServerPlayerGameMode::useItemOn;

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!state.getBlock().isEnabled(level.enabledFeatures())) {
            return;
        }

        ItemStack usedItemStack = itemStack.copy();
        if (itemStack.isEmpty() || player.getCooldowns().isOnCooldown(itemStack)) {
            return;
        }

        UseOnContext context = new UseOnContext(level, player, InteractionHand.MAIN_HAND, itemStack, hitResult);
        InteractionResult success;
        if (player.hasInfiniteMaterials()) {
            int count = itemStack.getCount();
            success = itemStack.useOn(context);
            itemStack.setCount(count);
        } else {
            success = itemStack.useOn(context);
        }

        if (success.consumesAction()) {
            CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, pos, usedItemStack);
        }
    }

    public static void hoe3x3(Level level, ServerPlayer player, BlockPos pos, BlockState state, InteractionHand hand) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                var newPos = pos.offset(i - 1, 0, j - 1);
                if (!newPos.equals(pos)) {
                    var currentState = level.getBlockState(newPos);
                    if (currentState.is(state.getBlock())) {
                        hoeBlock(level, player, newPos, hand);
                    }
                }
            }
        }
    }

    public static void hoeBlock(Level level, ServerPlayer player, BlockPos pos, InteractionHand hand) {
        if (player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) {
            return;
        }

        Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> logicPair = HoeItem.TILLABLES.get(level.getBlockState(pos).getBlock());
        var item = player.getItemInHand(hand);
        UseOnContext context = new UseOnContext(level, player, hand, item, new BlockHitResult(Vec3.ZERO, Direction.UP, pos, false));

        if (logicPair == null) {
            // Fallback: if mapping removed some tillable entries, try invoking the hoe's useOn logic directly.
            try {
                InteractionResult res = item.useOn(context);
                if (res != null && res.consumesAction()) {
                    level.playSound(null, pos, SoundEvents.HOE_TILL.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    if (!level.isClientSide()) {
                        item.hurtAndBreak(1, player, hand.asEquipmentSlot());
                    }
                }
            } catch (Exception ignored) {
                // If direct use fails, nothing we can do — silently ignore to avoid crashes.
            }
            return;
        }

        Predicate<UseOnContext> predicate = logicPair.getFirst();
        Consumer<UseOnContext> action = logicPair.getSecond();
        if (predicate.test(context)) {
            level.playSound(null, pos, SoundEvents.HOE_TILL.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!level.isClientSide()) {
                action.accept(context);
                item.hurtAndBreak(1, player, hand.asEquipmentSlot());
            }
        }
    }
}
