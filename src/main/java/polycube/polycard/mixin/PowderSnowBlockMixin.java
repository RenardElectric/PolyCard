package polycube.polycard.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import polycube.polycard.events.callBacks.WalkOnPowderSnowEventCallback;

/// Lets card effects override PowderSnowBlock's walkability check.
@Mixin(PowderSnowBlock.class)
public class PowderSnowBlockMixin {
    @WrapMethod(method = "canEntityWalkOnPowderSnow")
    private static boolean canWalk(Entity entity, Operation<Boolean> original) {
        var result = original.call(entity);
        var callbackResult = WalkOnPowderSnowEventCallback.EVENT.invoker().interact(entity, result);
        if (callbackResult != InteractionResult.PASS) {
            return callbackResult == InteractionResult.SUCCESS;
        }
        return result;
    }
}
