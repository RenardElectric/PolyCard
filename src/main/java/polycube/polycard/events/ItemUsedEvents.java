package polycube.polycard.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import polycube.polycard.events.callBacks.ItemUsedEventCallback;

import java.util.ArrayList;
import java.util.List;

public class ItemUsedEvents {
    private static final List<ItemUsedEvent> ITEM_USED_EVENTS = new ArrayList<>();

    public static void registerItemUsedEvents(ItemUsedEvent... itemUsedEvents) {
        if (ITEM_USED_EVENTS.isEmpty()) {
            ItemUsedEventCallback.EVENT.register((player, world, hand, itemUseResult) -> {
                if (!(itemUseResult instanceof InteractionResult.Success)) {
                    return InteractionResult.PASS;
                }

                for (ItemUsedEvent event : ITEM_USED_EVENTS) {
                    var result = event.handle(player, world, hand);
                    if (result ==  InteractionResult.SUCCESS) {
                        return InteractionResult.SUCCESS;
                    } else if (result == InteractionResult.FAIL) {
                        return InteractionResult.FAIL;
                    }
                }

                return InteractionResult.PASS;
            });
        }

        ITEM_USED_EVENTS.addAll(List.of(itemUsedEvents));
    }

    @FunctionalInterface
    public interface ItemUsedEvent {
        InteractionResult handle(ServerPlayer player, Level world, InteractionHand hand);
    }
}
