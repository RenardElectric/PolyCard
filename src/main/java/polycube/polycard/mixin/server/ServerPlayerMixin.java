package polycube.polycard.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polycube.polycard.events.callBacks.GetBedRuleEventCallback;
import polycube.polycard.events.callBacks.KeepInventoryEventCallback;
import polycube.polycard.utils.EffectHelpers;

import java.util.Collection;
import java.util.Optional;

/// Prevents transient managed effects from becoming permanent during keep-effects respawns.
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Shadow
    public abstract ServerLevel level();

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


    @WrapOperation(method = "findRespawnAndUseSpawnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/AbstractBedBlock;getBedRule(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/attribute/BedRule;"))
    private static BedRule getBedRuleStatic(AbstractBedBlock instance, Level level, BlockPos pos, Operation<BedRule> original) {
        return GetBedRuleEventCallback.EVENT.invoker().getBedRule(player, original.call(instance, level, pos));
    }

    @WrapOperation(method = "restoreFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
    private Object getKeepInventory(GameRules instance, GameRule<Boolean> gameRule, Operation<Boolean> original) {
        boolean orig = original.call(instance, gameRule);
        var result = KeepInventoryEventCallback.EVENT.invoker().onKeepInventory((ServerPlayer) (Object) this, this.level(), orig);
        return result == InteractionResult.SUCCESS || (result != InteractionResult.FAIL && orig);
    }
}
