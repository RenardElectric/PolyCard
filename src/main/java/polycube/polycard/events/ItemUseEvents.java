package polycube.polycard.events;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class ItemUseEvents {
    private static final List<ItemUseEvent> ITEM_USE_EVENTS = new ArrayList<>();

    public static void registerItemUseEvents(ItemUseEvent... itemUseEvents) {
        if (ITEM_USE_EVENTS.isEmpty()) {
            UseItemCallback.EVENT.register((player, world, hand) -> {
                if (world.isClientSide()) {
                    return InteractionResult.PASS;
                }

                if (player.isSpectator()) {
                    return InteractionResult.PASS;
                }

                var serverPlayer = (ServerPlayer) player;

                for (ItemUseEvent event : ITEM_USE_EVENTS) {
                    var result = event.handle(serverPlayer, world, hand);
                    if (result ==  InteractionResult.SUCCESS) {
                        return InteractionResult.SUCCESS;
                    } else if (result == InteractionResult.FAIL) {
                        return InteractionResult.FAIL;
                    }
                }

                return InteractionResult.PASS;
            });
        }

        ITEM_USE_EVENTS.addAll(List.of(itemUseEvents));
    }

    @FunctionalInterface
    public interface ItemUseEvent {
        InteractionResult handle(ServerPlayer player, Level world, InteractionHand hand);
    }
}
