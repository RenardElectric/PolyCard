package polycube.polycard.events;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import polycube.polycard.events.callBacks.ItemConsumedEventCallback;

import java.util.ArrayList;
import java.util.List;

public class ItemConsumedEvents {
    private static final List<ItemConsumedEvent> ITEM_CONSUMED_EVENTS = new ArrayList<>();

    public static void registerItemUseEvents(ItemConsumedEvent... itemConsumedEvents) {
        if (ITEM_CONSUMED_EVENTS.isEmpty()) {
            ItemConsumedEventCallback.EVENT.register((player, itemStack) -> {
                for (ItemConsumedEvent event : ITEM_CONSUMED_EVENTS) {
                    var result = event.handle(player, itemStack);
                    if (result ==  InteractionResult.SUCCESS) {
                        return InteractionResult.SUCCESS;
                    } else if (result == InteractionResult.FAIL) {
                        return InteractionResult.FAIL;
                    }
                }

                return InteractionResult.PASS;
            });
        }

        ITEM_CONSUMED_EVENTS.addAll(List.of(itemConsumedEvents));
    }

    @FunctionalInterface
    public interface ItemConsumedEvent {
        InteractionResult handle(ServerPlayer player, ItemStack itemStack);
    }
}
