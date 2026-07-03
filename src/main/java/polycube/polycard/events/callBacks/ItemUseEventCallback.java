package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

/// Server-side wrapper around Fabric's UseItemCallback.
/// Non-PASS results are returned to Fabric and also trigger an inventory slot sync.
public interface ItemUseEventCallback {
    Event<ItemUseEventCallback> EVENT = EventFactory.createArrayBacked(ItemUseEventCallback.class,
            (listeners) -> (player, level, hand) -> {
                if (player.isSpectator()) {
                    return InteractionResult.PASS;
                }

                for (var listener : listeners) {
                    var result = listener.onItemUse(player, level, hand);
                    if (result != InteractionResult.PASS) {
                        int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;
                        player.connection.send(player.getInventory().createInventoryUpdatePacket(slot));
                        return result;
                    }
                }

                return InteractionResult.PASS;
            });

    InteractionResult onItemUse(ServerPlayer player, Level world, InteractionHand hand);
}
