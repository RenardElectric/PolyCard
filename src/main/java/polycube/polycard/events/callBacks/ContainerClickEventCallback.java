package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;

/// Fired before a server player's click in a container menu is handled.
/// PASS allows vanilla handling; any non-PASS result cancels the click.
/// Listeners must not open or close menus directly: the click packet is still being processed.
public interface ContainerClickEventCallback {
    Event<ContainerClickEventCallback> EVENT = EventFactory.createArrayBacked(ContainerClickEventCallback.class,
            (listeners) -> (player, menu, slotId, button, input) -> {
                for (var listener : listeners) {
                    InteractionResult result = listener.onContainerClick(player, menu, slotId, button, input);

                    if (!result.equals(InteractionResult.PASS)) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            });

    InteractionResult onContainerClick(ServerPlayer player, AbstractContainerMenu menu, int slotId, int button, ContainerInput input);
}
