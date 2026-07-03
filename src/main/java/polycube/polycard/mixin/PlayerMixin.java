package polycube.polycard.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;

/// Exposes player damage to card effects and writes back damage mutations.
@Mixin(Player.class)
public abstract class PlayerMixin extends Avatar implements ContainerUser {
    protected PlayerMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Unique
    private MutableFloat modifiedDamage = null;

    // Player hurtServer has a different local-variable shape from LivingEntity, so it needs its own hook.
    @ModifyVariable(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;scalesWithDifficulty()Z"), argsOnly = true, name = "damage")
    private float modifyDamage(float damage) {
        if (modifiedDamage != null) {
            damage = modifiedDamage.floatValue();
            modifiedDamage = null;
        }
        return damage;
    }

    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;removeEntitiesOnShoulder()V"), cancellable = true)
    private void onHurt(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        modifiedDamage = new MutableFloat(damage);
        if (EntityHurtEventCallback.EVENT.invoker().onEntityHurt(this, level, source, modifiedDamage) == InteractionResult.FAIL) {
            cir.setReturnValue(false);
        }
    }
}
