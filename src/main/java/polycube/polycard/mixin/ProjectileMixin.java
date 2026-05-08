package polycube.polycard.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polycube.polycard.events.callBacks.ProjectileOnHitEventCallback;

@Mixin(Projectile.class)
public abstract class ProjectileMixin extends Entity implements TraceableEntity {
    public ProjectileMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void onTrigger(HitResult hitResult, CallbackInfo ci) {
        var result = ProjectileOnHitEventCallback.EVENT.invoker().interact((Projectile)(Object)this, hitResult); // Trick the compiler into accepting this mixin as a Projectile
        if (result != net.minecraft.world.InteractionResult.PASS) {
            ci.cancel();
        }
    }
}
