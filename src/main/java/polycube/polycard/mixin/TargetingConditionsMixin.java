package polycube.polycard.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import polycube.polycard.events.callBacks.IsTargetedEventCallback;

/// Lets card effects override mob targeting without replacing the original targeting predicate.
@Mixin(TargetingConditions.class)
public abstract class TargetingConditionsMixin {

    @Shadow
    @Final
    private boolean isCombat;

    @Shadow
    private double range = -1.0;

    @Shadow
    private boolean checkLineOfSight = true;

    @Shadow
    private boolean testInvisible = true;

    @Shadow
    private TargetingConditions.@Nullable Selector selector;

    @WrapMethod(method = "test")
    private boolean isTargeted(ServerLevel level, @Nullable LivingEntity targeter, LivingEntity target, Operation<Boolean> original) {
        var result = original.call(level, targeter, target);
        var targetingConditionsData = new IsTargetedEventCallback.TargetingConditionsData(isCombat, range, checkLineOfSight, testInvisible, selector, result);
        var interactionResult = IsTargetedEventCallback.EVENT.invoker().onTargeted(level, targeter, target, targetingConditionsData);
        return interactionResult != InteractionResult.FAIL && (interactionResult == InteractionResult.SUCCESS || result);
    }
}
