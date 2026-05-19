package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import polycube.polycard.mixin.TargetingConditionsMixin;

public interface ItemDurabilityChangeEventCallback {
    Event<ItemDurabilityChangeEventCallback> EVENT = EventFactory.createArrayBacked(ItemDurabilityChangeEventCallback.class,
            (listeners) -> (level, player, itemStack, amount) -> {
                for (var listener : listeners) {
                    amount = listener.interact(level, player, itemStack, amount);
                }
                return amount;
            });

    int interact(ServerLevel level, @Nullable ServerPlayer player, ItemStack itemStack, int amount);
}
