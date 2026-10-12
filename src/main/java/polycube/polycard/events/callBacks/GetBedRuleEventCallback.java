package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/// Callback for overriding the bed rule for a player.
public interface GetBedRuleEventCallback {
    Event<GetBedRuleEventCallback> EVENT = EventFactory.createArrayBacked(
            GetBedRuleEventCallback.class,
            listeners -> (player, level, original) -> {
                for (var listener : listeners) {
                    original = listener.getBedRule(player, level, original);
                }
                return original;
            }
    );

    BedRule getBedRule(@Nullable Player player, Level level, BedRule original);
}
