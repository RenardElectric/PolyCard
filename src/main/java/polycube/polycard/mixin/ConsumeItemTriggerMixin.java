package polycube.polycard.mixin;

import net.minecraft.advancements.criterion.ConsumeItemTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polycube.polycard.events.callBacks.ItemConsumedEventCallback;

/// Reuses vanilla's consume-item advancement trigger before item effects are applied.
@Mixin(ConsumeItemTrigger.class)
public abstract class ConsumeItemTriggerMixin {
    @Inject(method = "trigger", at = @At("HEAD"))
    private void itemConsumed(ServerPlayer player, ItemStack itemStack, CallbackInfo ci) {
        ItemConsumedEventCallback.EVENT.invoker().interact(player, itemStack);
    }
}
