package polycube.polycard.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.effect.MobEffectInstance;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import polycube.polycard.utils.EffectHelpers;
import polycube.polycard.utils.MobEffectInstanceDuck;

/// Keeps transient persistent-effect ownership intact across vanilla chain mutations.
@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin implements MobEffectInstanceDuck {
    @Unique
    private @Nullable Object polycard$persistentEffectOwner;

    @Override
    public @Nullable Object polycard$getPersistentEffectOwner() {
        return polycard$persistentEffectOwner;
    }

    @Override
    public void polycard$setPersistentEffectOwner(@Nullable Object owner) {
        polycard$persistentEffectOwner = owner;
    }

    @WrapMethod(method = "update")
    private boolean polycard$trackPersistentEffectUpdate(MobEffectInstance takeOver, Operation<Boolean> original) {
        var current = (MobEffectInstance) (Object) this;
        int previousAmplifier = current.getAmplifier();
        var previousOwner = EffectHelpers.persistentEffectOwner(current);
        var incomingOwner = EffectHelpers.persistentEffectOwner(takeOver);

        if (EffectHelpers.beforeEffectUpdate(current, takeOver)) {
            return true;
        }
        var hiddenBeforeUpdate = current.hiddenEffect;
        boolean changed = original.call(takeOver);
        EffectHelpers.afterEffectUpdate(current, previousAmplifier, previousOwner, incomingOwner, hiddenBeforeUpdate);
        return changed;
    }

    @WrapMethod(method = "downgradeToHiddenEffect")
    private boolean polycard$trackPersistentEffectPromotion(Operation<Boolean> original) {
        var current = (MobEffectInstance) (Object) this;
        var promotedEffect = current.hiddenEffect;
        var promotedOwner = EffectHelpers.persistentEffectOwner(promotedEffect);
        boolean promoted = original.call();
        EffectHelpers.afterHiddenEffectPromotion(current, promotedEffect, promotedOwner, promoted);
        return promoted;
    }
}
