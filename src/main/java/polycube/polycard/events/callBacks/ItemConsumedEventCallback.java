package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/// Fired by the consume-item advancement trigger before the consumed item's effects are applied.
public interface ItemConsumedEventCallback {
    Event<ItemConsumedEventCallback> EVENT = EventFactory.createArrayBacked(ItemConsumedEventCallback.class,
            (listeners) -> (player, itemStack) -> {
                for (var listener : listeners) {
                    listener.interact(player, itemStack);
                }
            });

    void interact(ServerPlayer player, ItemStack itemStack);
}
