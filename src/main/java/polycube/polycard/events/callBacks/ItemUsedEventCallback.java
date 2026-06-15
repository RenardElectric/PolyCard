package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

/// Fired after ItemStack.use returns a successful result on the server.
public interface ItemUsedEventCallback {
    Event<ItemUsedEventCallback> EVENT = EventFactory.createArrayBacked(ItemUsedEventCallback.class,
            (listeners) -> (player, level, hand, itemUseResult) -> {
                if (!(itemUseResult instanceof InteractionResult.Success)) {
                    return;
                }

                for (var listener : listeners) {
                    listener.interact(player, level, hand, itemUseResult);
                }
            });

    void interact(ServerPlayer player, ServerLevel level, InteractionHand hand, InteractionResult itemUseResult);
}
