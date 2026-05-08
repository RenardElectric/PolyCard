package polycube.polycard.mixin;

import net.minecraft.advancements.criterion.KilledTrigger;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polycube.polycard.events.killEvents.KillEventCallback;

@Mixin(KilledTrigger.class)
public abstract class KilledTriggerMixin extends SimpleCriterionTrigger<KilledTrigger.TriggerInstance> {
    @Inject(method = "trigger", at = @At("HEAD"))
    private void onTrigger(ServerPlayer player, Entity entity, DamageSource killingBlow, CallbackInfo ci) {
        KillEventCallback.EVENT.invoker().interact(player, entity, killingBlow);
    }
}
