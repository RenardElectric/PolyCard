package polycube.polycard.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polycube.polycard.utils.EffectHelpers;

import java.util.Collection;

/// Prevents transient managed effects from becoming permanent during keep-effects respawns.
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "restoreFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeMap;assignPermanentModifiers(Lnet/minecraft/world/entity/ai/attributes/AttributeMap;)V", shift = At.Shift.AFTER))
    private void reconcileTransientPersistentEffectAttributes(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci) {
        EffectHelpers.reconcileCopiedEffectAttributes((ServerPlayer) (Object) this, oldPlayer.getActiveEffects());
    }

    @ModifyExpressionValue(method = "restoreFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getActiveEffects()Ljava/util/Collection;"))
    private Collection<MobEffectInstance> excludeTransientPersistentEffects(Collection<MobEffectInstance> effects) {
        return EffectHelpers.effectsForRespawn(effects);
    }
}
