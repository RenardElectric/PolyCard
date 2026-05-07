package polycube.polycard.events.itemEvents;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public class ItemEvents {
    public static void registerItemEvents(ItemEvent... itemEvents) {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }

            var serverPlayer = (ServerPlayer) player;

            for (ItemEvent event : itemEvents) {
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
}
