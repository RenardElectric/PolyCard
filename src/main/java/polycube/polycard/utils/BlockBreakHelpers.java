package polycube.polycard.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public final class BlockBreakHelpers {
    private BlockBreakHelpers() {}

    public static void breakBlock(ServerLevel level, ServerPlayer player, BlockPos pos, boolean swingHand, QadriConsumer<Level, ServerPlayer, BlockPos, BlockState> onBreak) {
        //code from ServerPlayerGameMode::destroyBlock;
        var itemStack = player.getActiveItem();
        BlockState state = level.getBlockState(pos);
        if (!itemStack.canDestroyBlock(state, level, pos, player)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        Block block = state.getBlock();

        if (player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) {
            return;
        }

        level.levelEvent(null, 2001, pos, Block.getId(state)); // Break particles and sound
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));

        boolean changed = level.removeBlock(pos, false);

        if (changed) {
            block.destroy(level, pos, state);
            onBreak.accept(level, player, pos, state);
        }

        if (swingHand) {
            player.swing(player.getUsedItemHand(), SwingAnimation.DEFAULT, true);
        }

        if (player.preventsBlockDrops()) {
            return;
        }

        ItemStack destroyedWith = itemStack.copy();

        var tool = itemStack.get(DataComponents.TOOL);
        if (tool != null) {
            itemStack.hurtAndBreak(tool.damagePerBlock(), player, EquipmentSlot.MAINHAND);
            player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
        }

        if (changed) {
            block.playerDestroy(level, player, pos, state, blockEntity, destroyedWith);
        }
    }
}
