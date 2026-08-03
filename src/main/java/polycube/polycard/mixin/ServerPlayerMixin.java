package polycube.polycard.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.effect.MobEffectInstance;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polycube.polycard.events.callBacks.GetBedRuleEventCallback;
import polycube.polycard.utils.EffectHelpers;

import java.util.Collection;
import java.util.Optional;

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

    @Unique
    private static @Nullable ServerPlayer player;

    @WrapOperation(method = "findRespawnPositionAndUseSpawnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnAndUseSpawnBlock(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer$RespawnConfig;Z)Ljava/util/Optional;"))
    private Optional<ServerPlayer.RespawnPosAngle> getPlayer(ServerLevel level, ServerPlayer.RespawnConfig respawnConfig, boolean consumeSpawnBlock, Operation<Optional<ServerPlayer.RespawnPosAngle>> original) {
        player = (ServerPlayer) (Object) this;
        var result = original.call(level, respawnConfig, consumeSpawnBlock);
        player = null;
        return result;
    }


    @WrapOperation(method = "findRespawnAndUseSpawnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/core/BlockPos;)Ljava/lang/Object;"))
    private static Object getBedRuleStatic(EnvironmentAttributeSystem instance, EnvironmentAttribute<BedRule> environmentAttribute, BlockPos blockPos, Operation<Object> original) {
        if (player == null) {
            return original.call(instance, environmentAttribute, blockPos);
        }
        return GetBedRuleEventCallback.EVENT.invoker().getBedRule(player, (BedRule) original.call(instance, environmentAttribute, blockPos));
    }

    @WrapOperation(method = "startSleepInBed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/core/BlockPos;)Ljava/lang/Object;"))
    private Object getBedRule(EnvironmentAttributeSystem instance, EnvironmentAttribute<BedRule> environmentAttribute, BlockPos blockPos, Operation<Object> original) {
        return GetBedRuleEventCallback.EVENT.invoker().getBedRule((ServerPlayer) (Object) this, (BedRule) original.call(instance, environmentAttribute, blockPos));
    }
}
