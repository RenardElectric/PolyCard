package polycube.polycard.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import polycube.polycard.events.callBacks.GetBedRuleEventCallback;

@Mixin(BedBlock.class)
public abstract class BedBlockMixin extends HorizontalDirectionalBlock {
    protected BedBlockMixin(Properties properties) {
        super(properties);
    }

    @WrapOperation(method = "useWithoutItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/core/BlockPos;)Ljava/lang/Object;"))
    private Object allowSleepingInBed(EnvironmentAttributeSystem instance, EnvironmentAttribute<BedRule> environmentAttribute, BlockPos blockPos, Operation<Object> original, @Local(argsOnly = true, name = "player") Player player) {
        return GetBedRuleEventCallback.EVENT.invoker().getBedRule(player, (BedRule) original.call(instance, environmentAttribute, blockPos));
    }
}
