package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/// Callback for consuming an item.
/// Called after the item is consumed but before effects are applied
/// Upon return:
/// - SUCCESS cancels further processing and continues with normal item consume behavior.
/// - PASS falls back to further processing and defaults to SUCCESS if no other listeners are available
public interface ItemConsumedEventCallback {
    Event<ItemConsumedEventCallback> EVENT = EventFactory.createArrayBacked(ItemConsumedEventCallback.class,
            (listeners) -> (player, itemStack) -> {
                for (ItemConsumedEventCallback listener : listeners) {
                    InteractionResult result = listener.used(player, itemStack);

                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            });

    InteractionResult used(ServerPlayer player, ItemStack itemStack);
}
