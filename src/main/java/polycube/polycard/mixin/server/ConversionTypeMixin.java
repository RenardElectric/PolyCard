package polycube.polycard.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ConversionType;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import polycube.polycard.utils.EffectHelpers;

import java.util.Collection;

/// Prevents transient managed effects from becoming permanent when a mob is converted.
@Mixin(ConversionType.class)
public abstract class ConversionTypeMixin {
    @WrapOperation(method = "convertCommon", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;getAbsorptionAmount()F"))
    private static float excludeTransientPersistentEffectAbsorption(Mob from, Operation<Float> original) {
        return EffectHelpers.externalAbsorptionAmount(from, original.call(from));
    }

    @ModifyExpressionValue(method = "convertCommon", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;getActiveEffects()Ljava/util/Collection;"))
    private static Collection<MobEffectInstance> excludeTransientPersistentEffects(Collection<MobEffectInstance> effects) {
        return EffectHelpers.effectsForConversion(effects);
    }
}
