package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/// Callback for overriding the bed rule for a player.
public interface GetBedRuleEventCallback {
    Event<GetBedRuleEventCallback> EVENT = EventFactory.createArrayBacked(
            GetBedRuleEventCallback.class,
            listeners -> (player, original) -> {
                for (var listener : listeners) {
                    original = listener.getBedRule(player, original);
                }
                return original;
            }
    );

    BedRule getBedRule(@Nullable Player player, BedRule original);
}
