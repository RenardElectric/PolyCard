package polycube.polycard.events.cardLootEvents;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import polycube.polycard.card.CardType;
import polycube.polycard.utils.CardHelper;

import static net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL;

public class UseItemOnEvents {
    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player.isSpectator() || world.isClientSide()) {
                return InteractionResult.PASS;
            }

            var state = world.getBlockState(hitResult.getBlockPos());
            var itemStack = player.getItemInHand(hand);
            var serverPlayer = (ServerPlayer) player;

            getBeeCard(serverPlayer, state, itemStack);

            return InteractionResult.PASS;
        });
    }

    private static void getBeeCard(ServerPlayer player, BlockState state, ItemStack itemStack) {
        if (state.is(Blocks.BEEHIVE) || state.is(Blocks.BEE_NEST)) {
            if (state.getValue(HONEY_LEVEL) >= 5) {
                if (itemStack.is(Items.GLASS_BOTTLE)) {
                    CardHelper.receiveCard(player, CardType.BEE);
                }
            }
        }
    }
}
