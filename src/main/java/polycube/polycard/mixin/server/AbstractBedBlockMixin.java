package polycube.polycard.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import polycube.polycard.events.callBacks.GetBedRuleEventCallback;

@Mixin(AbstractBedBlock.class)
public abstract class AbstractBedBlockMixin extends HorizontalDirectionalBlock {
    protected AbstractBedBlockMixin(Properties properties) {
        super(properties);
    }

    @WrapOperation(method = "useWithoutItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/AbstractBedBlock;getBedRule(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/attribute/BedRule;"))
    private BedRule allowSleepingInBed(AbstractBedBlock instance, Level level, BlockPos pos, Operation<BedRule> original, @Local(argsOnly = true, name = "player") Player player) {
        return GetBedRuleEventCallback.EVENT.invoker().getBedRule(player, original.call(instance, level, pos));
    }

    @WrapOperation(method = "onStopSleeping", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/AbstractBedBlock;getBedRule(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/attribute/BedRule;"))
    private BedRule doesBedDestroy(AbstractBedBlock instance, Level level, BlockPos pos, Operation<BedRule> original) {
        return GetBedRuleEventCallback.EVENT.invoker().getBedRule(null, original.call(instance, level, pos));
    }
}
