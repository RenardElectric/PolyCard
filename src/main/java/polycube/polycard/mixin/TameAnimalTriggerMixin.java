package polycube.polycard.mixin;

import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.advancements.triggers.TameAnimalTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polycube.polycard.events.callBacks.TameEventCallback;

/// Reuses vanilla's taming advancement trigger as the source of card-award events.
@Mixin(TameAnimalTrigger.class)
public abstract class TameAnimalTriggerMixin extends SimpleCriterionTrigger<TameAnimalTrigger.TriggerInstance> {
    @Inject(method = "trigger", at = @At("HEAD"))
    private void animalTamed(ServerPlayer player, Animal animal, CallbackInfo ci) {
        TameEventCallback.EVENT.invoker().onTame(player, animal);
    }
}
