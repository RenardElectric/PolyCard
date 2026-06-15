package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

/// Server-side wrapper around Fabric's UseItemCallback.
/// Non-PASS results are returned to Fabric and also trigger an inventory slot sync.
public interface ItemUseEventCallback {
    static void register(ItemUseEventCallback itemUseEvent) {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide() || player.isSpectator()) {
                return InteractionResult.PASS;
            }
            var serverPlayer = (ServerPlayer) player;

            var result = itemUseEvent.interact(serverPlayer, world, hand);
            if (result != InteractionResult.PASS) {
                int slot = hand == InteractionHand.MAIN_HAND ? serverPlayer.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;
                serverPlayer.connection.send(player.getInventory().createInventoryUpdatePacket(slot));
            }
            return result;
        });
    }

    InteractionResult interact(ServerPlayer player, Level world, InteractionHand hand);
}
