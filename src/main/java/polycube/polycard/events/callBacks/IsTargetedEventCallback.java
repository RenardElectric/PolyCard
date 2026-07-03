package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jspecify.annotations.Nullable;

/// Lets card effects override the result of TargetingConditions.test.
/// PASS keeps the vanilla result, SUCCESS forces targetable, and FAIL forces untargetable.
public interface IsTargetedEventCallback {
    Event<IsTargetedEventCallback> EVENT = EventFactory.createArrayBacked(IsTargetedEventCallback.class,
            (listeners) -> (level, targeter, target, targetingConditions) -> {
                for (var listener : listeners) {
                    InteractionResult interactionResult = listener.onTargeted(level, targeter, target, targetingConditions);

                    if (interactionResult != InteractionResult.PASS) {
                        return interactionResult;
                    }
                }

                return InteractionResult.PASS;
            });

    InteractionResult onTargeted(ServerLevel level, @Nullable LivingEntity targeter, LivingEntity target, TargetingConditionsData targetingConditions);

    record TargetingConditionsData(
            boolean isCombat, double range, boolean checkLineOfSight, boolean skipInvisible,
            TargetingConditions.@Nullable Selector selector, boolean originalResult
    ) { }
}
