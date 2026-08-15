package polycube.polycard.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import polycube.polycard.events.callBacks.ExplosionKnockbackEventCallback;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin implements Explosion {

    @WrapOperation(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ExplosionDamageCalculator;getKnockbackMultiplier(Lnet/minecraft/world/entity/Entity;)F"))
    private static float knockbackMultiplier(ExplosionDamageCalculator instance, Entity entity, Operation<Float> original) {
        return ExplosionKnockbackEventCallback.EVENT.invoker().onExplosionKnockback(entity, original.call(instance, entity));
    }

}
