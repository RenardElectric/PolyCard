package polycube.polycard.mixin.server;

import net.minecraft.advancements.triggers.BredAnimalsTrigger;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polycube.polycard.events.callBacks.BreedEventCallback;

import java.util.Optional;

/// Reuses vanilla's breeding advancement trigger as the source of card-award events.
@Mixin(BredAnimalsTrigger.class)
public abstract class BredAnimalsTriggerMixin extends SimpleCriterionTrigger<BredAnimalsTrigger.TriggerInstance> {
    @Inject(method = "trigger", at = @At("HEAD"))
    private void animalBred(ServerPlayer player, Animal parent, Animal partner, @Nullable AgeableMob child, CallbackInfo ci) {
        BreedEventCallback.EVENT.invoker().onBreed(player, parent, partner, Optional.ofNullable(child));
    }
}
