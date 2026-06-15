package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

/// Callback for using an item.
/// Called after the item is used.
/// Upon return:
/// - SUCCESS cancels further processing and continues with normal item use behavior.
/// - PASS falls back to further processing and defaults to SUCCESS if no other listeners are available
public interface ItemUsedEventCallback {
    Event<ItemUsedEventCallback> EVENT = EventFactory.createArrayBacked(ItemUsedEventCallback.class,
            (listeners) -> (player, level, hand, itemUseResult) -> {
                if (!(itemUseResult instanceof InteractionResult.Success)) {
                    return;
                }

                for (var listener : listeners) {
                    listener.interact(player, level, hand, itemUseResult);
                }

                return;
            });

    void interact(ServerPlayer player, ServerLevel level, InteractionHand hand, InteractionResult itemUseResult);
}
