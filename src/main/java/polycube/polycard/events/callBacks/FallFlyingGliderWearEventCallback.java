package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/// Fired only for vanilla's periodic durability wear while a server player is fall-flying.
/// Returning true cancels that one durability operation.
public interface FallFlyingGliderWearEventCallback {
    Event<FallFlyingGliderWearEventCallback> EVENT = EventFactory.createArrayBacked(
            FallFlyingGliderWearEventCallback.class,
            listeners -> (player, glider, slot) -> {
                for (var listener : listeners) {
                    if (listener.cancelGliderWear(player, glider, slot)) {
                        return true;
                    }
                }
                return false;
            }
    );

    boolean cancelGliderWear(ServerPlayer player, ItemStack glider, EquipmentSlot slot);
}
