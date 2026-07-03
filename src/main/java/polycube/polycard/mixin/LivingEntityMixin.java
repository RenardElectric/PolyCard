package polycube.polycard.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Attackable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;

/// Exposes non-player LivingEntity damage to card effects and writes back damage mutations.
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements Attackable, WaypointTransmitter {
    public LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Unique
    private MutableFloat modifiedDamage = null;

    // The callback runs before vanilla consumes the damage variable; this hook writes back any mutation.
    @ModifyVariable(method = "hurtServer", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;noActionTime:I", opcode = Opcodes.PUTFIELD), argsOnly = true, name = "damage")
    private float modifyDamage(float damage) {
        if (modifiedDamage != null) {
            damage = modifiedDamage.floatValue();
            modifiedDamage = null;
        }
        return damage;
    }

    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"), cancellable = true)
    private void onHurt(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        modifiedDamage = new MutableFloat(damage);
        var entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player)) {
            if (EntityHurtEventCallback.EVENT.invoker().onEntityHurt(entity, level, source, modifiedDamage) == InteractionResult.FAIL) {
                cir.setReturnValue(false);
            }
        }
    }
}
