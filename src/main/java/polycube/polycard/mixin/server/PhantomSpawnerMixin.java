package polycube.polycard.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import polycube.polycard.events.callBacks.AllowPhantomSpawnEventCallback;

@Mixin(PhantomSpawner.class)
public abstract class PhantomSpawnerMixin implements CustomSpawner {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z"))
    private boolean allowPhantomSpawn(ServerPlayer instance, Operation<Boolean> original, @Local(argsOnly = true, name = "level") ServerLevel level) {
        return original.call(instance) || !AllowPhantomSpawnEventCallback.EVENT.invoker().allowPhantomSpawn(instance, level);
    }
}
