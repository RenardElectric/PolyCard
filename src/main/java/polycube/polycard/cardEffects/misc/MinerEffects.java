package polycube.polycard.cardEffects.misc;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import polycube.polycard.PolyCard;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.utils.BlockBreakHelpers;
import polycube.polycard.utils.QadriConsumer;

import java.util.HashSet;
import java.util.function.Predicate;

public class MinerEffects extends CardEffects implements PlayerBlockBreakEvents.After {

    public static final TagKey<Block> VEIN_MINABLE_BLOCKS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, "vein_mineable_blocks"));
    public static final TagKey<Block> MINABLE_BLOCKS_3X3 = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, "mineable_blocks_3x3"));

    public static final int VEIN_MINE_BLOCK_COUNT = 16;

    @Override
    public void afterBlockBreak(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (player instanceof ServerPlayer serverPlayer) {
            var tool = player.getActiveItem().get(DataComponents.TOOL);
            if (!player.isShiftKeyDown() && tool != null && tool.isCorrectForDrops(state)) {
                if (state.is(VEIN_MINABLE_BLOCKS) && hasCardOrRarer(serverPlayer, RarityLevel.EPIC)) {
                    veinMine(level, serverPlayer, pos, newState -> newState.is(state.getBlock()), QadriConsumer.empty());
                } else if (state.is(MINABLE_BLOCKS_3X3) && hasCardOrRarer(serverPlayer, RarityLevel.LEGENDARY)) {
                    mine3x3(level, serverPlayer, pos, newState -> newState.is(state.getBlock()), QadriConsumer.empty(), true);
                }
            }
        }
    }

    public static void mine3x3(Level level, ServerPlayer player, BlockPos pos, Predicate<BlockState> shouldMine, QadriConsumer<Level, ServerPlayer, BlockPos, BlockState> onBreak, boolean directional) {
        Vec3 directionToPlayer = player.getEyePosition().subtract(Vec3.atCenterOf(pos));
        var direction = Direction.getApproximateNearest(directionToPlayer);
        if (!directional) direction = Direction.UP;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                var newPos = switch (direction) {
                    case UP, DOWN -> pos.offset(i - 1, 0, j - 1);
                    case NORTH, SOUTH -> pos.offset(i - 1, j - 1, 0);
                    case EAST, WEST -> pos.offset(0, i - 1, j - 1);
                };
                if (!newPos.equals(pos)) {
                    BlockState newState = level.getBlockState(newPos);
                    if (shouldMine.test(newState)) {
                        BlockBreakHelpers.breakBlock(level, player, newPos, false, onBreak);
                    }
                }
            }
        }
    }

    public static void veinMine(Level level, ServerPlayer player, BlockPos pos, Predicate<BlockState> shouldMine, QadriConsumer<Level, ServerPlayer, BlockPos, BlockState> onBreak) {
        var posToMine = new HashSet<BlockPos>();
        var posToExplore = new HashSet<BlockPos>();
        posToExplore.add(pos);

        loop:
        while (!posToExplore.isEmpty()) {
            var posToExploreNext = new HashSet<BlockPos>();
            for (BlockPos explorePos : posToExplore) {
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        for (int k = 0; k < 3; k++) {
                            var newPos = explorePos.offset(i - 1, j - 1, k - 1);
                            if (newPos.equals(pos)) continue;
                            if (shouldMine.test(level.getBlockState(newPos)) && posToMine.add(newPos)) {
                                posToExploreNext.add(newPos);
                                if (posToMine.size() >= VEIN_MINE_BLOCK_COUNT) break loop;
                            }
                        }
                    }
                }
            }
            posToExplore = posToExploreNext;
        }

        for (BlockPos minePos : posToMine) {
            BlockBreakHelpers.breakBlock(level, player, minePos, false, onBreak);
        }
    }
}
