package polycube.polycard.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import polycube.polycard.events.callBacks.IsTargetedEventCallback;

@Mixin(TargetingConditions.class)
public abstract class TargetingConditionsMixin {

    @WrapMethod(method = "test")
    private boolean onTrigger(ServerLevel level, @Nullable LivingEntity targeter, LivingEntity target, Operation<Boolean> original) {
        var result = original.call(level, targeter, target);
        var targetingConditions = ((TargetingConditions)(Object)this);
        var interactionResult = IsTargetedEventCallback.EVENT.invoker().interact(level, targeter, target, targetingConditions, result);
        return interactionResult != InteractionResult.FAIL && (interactionResult == InteractionResult.SUCCESS || result);
    }
}
