package polycube.polycard.events.itemEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface ItemEvent {
    InteractionResult handle(ServerPlayer player, Level world, InteractionHand hand);
}
