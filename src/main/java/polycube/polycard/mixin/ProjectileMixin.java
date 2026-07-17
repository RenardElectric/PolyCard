package polycube.polycard.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import polycube.polycard.events.callBacks.ProjectileOnHitEventCallback;

/// Lets card effects consume a projectile after Minecraft computes its server-side hit result.
@Mixin(Projectile.class)
public abstract class ProjectileMixin extends Entity implements TraceableEntity {
    public ProjectileMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @WrapOperation(method = "hitTargetOrDeflectSelf", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;onHit(Lnet/minecraft/world/phys/HitResult;)V"))
    private void projectileHit(Projectile projectile, HitResult hitResult, Operation<Void> original) {
        if (!(projectile.level() instanceof ServerLevel)) {
            original.call(projectile, hitResult);
            return;
        }

        var result = ProjectileOnHitEventCallback.EVENT.invoker().onProjectileHit(projectile, hitResult);
        if (!result.equals(InteractionResult.PASS)) {
            projectile.discard();
            return;
        }
        original.call(projectile, hitResult);
    }
}
