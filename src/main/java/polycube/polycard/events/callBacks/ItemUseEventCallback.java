package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;

public interface ItemUseEventCallback {
    static void register(ItemUseEventCallback itemUseEvent) {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }

            if (player.isSpectator()) {
                return InteractionResult.PASS;
            }

            return itemUseEvent.interact((ServerPlayer) player, world, hand);
        });
    }

    InteractionResult interact(ServerPlayer player, Level world, InteractionHand hand);
}
