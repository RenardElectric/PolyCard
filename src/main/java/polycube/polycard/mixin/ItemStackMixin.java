package polycube.polycard.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import polycube.polycard.events.callBacks.ItemDurabilityChangeEventCallback;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @WrapMethod(method = "processDurabilityChange")
    public int durabilityChanged(int amount, ServerLevel level, ServerPlayer player, Operation<Integer> original) {
        return ItemDurabilityChangeEventCallback.EVENT.invoker().interact(level, player, (ItemStack) (Object) this, amount);
    }
}
