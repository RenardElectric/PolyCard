package polycube.polycard.mixin;

import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.advancements.triggers.SummonedEntityTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polycube.polycard.events.callBacks.EntitySummonedEventCallback;

/// Reuses vanilla's summon advancement trigger as the source of card-award events.
@Mixin(SummonedEntityTrigger.class)
public abstract class SummonedEntityTriggerMixin extends SimpleCriterionTrigger<SummonedEntityTrigger.TriggerInstance> {
    @Inject(method = "trigger", at = @At("HEAD"))
    public void entitySummoned(ServerPlayer player, Entity entity, CallbackInfo ci) {
        EntitySummonedEventCallback.EVENT.invoker().interact(player, entity);
    }
}
