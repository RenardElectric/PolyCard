package polycube.polycard.mixin;

import net.minecraft.advancements.triggers.KilledTrigger;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polycube.polycard.events.callBacks.PlayerKillEventCallback;

/// Reuses vanilla's kill advancement trigger as the source of card-award events.
@Mixin(KilledTrigger.class)
public abstract class KilledTriggerMixin extends SimpleCriterionTrigger<KilledTrigger.TriggerInstance> {
    @Inject(method = "trigger", at = @At("HEAD"))
    private void entityKilled(ServerPlayer player, Entity entity, DamageSource killingBlow, CallbackInfo ci) {
        PlayerKillEventCallback.EVENT.invoker().onPLayerKill(player, entity, killingBlow);
    }
}
