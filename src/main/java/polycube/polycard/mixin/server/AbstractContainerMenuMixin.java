package polycube.polycard.mixin.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polycube.polycard.events.callBacks.ContainerClickEventCallback;

/// Lets card effects intercept a server player's container clicks before vanilla handles them.
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void containerClicked(int slotId, int button, ContainerInput input, Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            var result = ContainerClickEventCallback.EVENT.invoker().onContainerClick(serverPlayer, (AbstractContainerMenu) (Object) this, slotId, button, input);
            if (!result.equals(InteractionResult.PASS)) {
                ci.cancel();
            }
        }
    }
}
