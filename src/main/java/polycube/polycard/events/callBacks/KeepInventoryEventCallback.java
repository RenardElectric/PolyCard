package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface KeepInventoryEventCallback {
    Event<KeepInventoryEventCallback> EVENT = EventFactory.createArrayBacked(KeepInventoryEventCallback.class,
            (listeners) -> (player, level, hand) -> {
                if (player.isSpectator()) {
                    return InteractionResult.PASS;
                }

                for (var listener : listeners) {
                    var result = listener.onKeepInventory(player, level, hand);
                    if (!result.equals(InteractionResult.PASS)) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            });

    InteractionResult onKeepInventory(Player player, Level world, boolean original);
}
