package polycube.polycard.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.PiglinSpecificSensor;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import polycube.polycard.PolyCard;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;

@Mixin(PiglinSpecificSensor.class)
public abstract class PiglinSpecificSensorMixin extends Sensor<LivingEntity> {
    @WrapOperation(method = "doTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/piglin/PiglinAi;isWearingSafeArmor(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    public boolean onTrigger(LivingEntity livingEntity, Operation<Boolean> original) {
        boolean isWearingSafeArmor = original.call(livingEntity);
        if (!isWearingSafeArmor && livingEntity instanceof Player player && PolyCard.cardManager != null) {
            return PolyCard.cardManager.getStorage().data(player).hasCardOrRarer(CardType.PIGLIN, RarityLevel.COMMON);
        }
        return true;
    }
}
