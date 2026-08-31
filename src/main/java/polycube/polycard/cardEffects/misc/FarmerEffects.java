package polycube.polycard.cardEffects.misc;

import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;

public class FarmerEffects extends CardEffects implements BlockEvents.UseItemOnCallback {
    @Override
    public @Nullable InteractionResult useItemOn(ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        if (player instanceof ServerPlayer serverPlayer && hasCardOrRarer(serverPlayer, RarityLevel.LEGENDARY)) {
            if (itemStack.getItem() instanceof HoeItem) {
                if (blockState.getBlock() instanceof CropBlock block && block.isMaxAge(blockState)) {
                    //code from ServerPlayerGameMode::destroyBlock;

                    BlockEntity blockEntity = level.getBlockEntity(blockPos);

                    if (player.blockActionRestricted(level, blockPos, serverPlayer.gameMode.getGameModeForPlayer())) {
                        return null;
                    }

                    level.levelEvent(null, 2001, blockPos, Block.getId(blockState)); // Break particles and sound
                    level.gameEvent(GameEvent.BLOCK_DESTROY, blockPos, GameEvent.Context.of(player, blockState));

                    boolean changed = level.removeBlock(blockPos, false);

                    if (changed) {
                        block.destroy(level, blockPos, blockState);
                        afterCropBreak(level, serverPlayer, blockPos, blockState);
                    }

                    serverPlayer.swing(interactionHand, true);

                    if (player.preventsBlockDrops()) {
                        return null;
                    }

                    ItemStack destroyedWith = itemStack.copy();

                    var tool = itemStack.get(DataComponents.TOOL);
                    if (tool != null) {
                        itemStack.hurtAndBreak(tool.damagePerBlock(), player, EquipmentSlot.MAINHAND);
                        player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                    }

                    if (changed) {
                        block.playerDestroy(level, player, blockPos, blockState, blockEntity, destroyedWith);
                    }
                }
            }
        }
        return null;
    }

    public void afterCropBreak(Level level, ServerPlayer player, BlockPos pos, BlockState state) {
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

    public void replantCrop(final ServerPlayer player, final Level level, final ItemStack itemStack, final BlockHitResult hitResult) {
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
}
