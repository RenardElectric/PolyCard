package polycube.polycard.mixin.server;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import polycube.polycard.events.callBacks.EntityAfterHurtEventCallback;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.GetBedRuleEventCallback;

import java.util.ArrayDeque;
import java.util.Deque;

/// Exposes validated player damage to card effects and writes back mutations.
@Mixin(Player.class)
public abstract class PlayerMixin extends Avatar implements ContainerUser {
    protected PlayerMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Unique
    private final Deque<MutableFloat> polycard$modifiedDamageStack = new ArrayDeque<>();

    @WrapMethod(method = "hurtServer")
    private boolean afterHurt(ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
        var player = (Player) (Object) this;
        float effectiveHealthBefore = player.getHealth() + player.getAbsorptionAmount();
        boolean accepted = original.call(level, source, damage);
        if (accepted) {
            float damageDealt = Math.max(0.0F, effectiveHealthBefore - player.getHealth() - player.getAbsorptionAmount());
            EntityAfterHurtEventCallback.EVENT.invoker().afterEntityHurt(player, level, source, damageDealt);
        }
        return accepted;
    }

    // Player has additional creative/dead guards before this point and applies difficulty scaling
    // afterward. Keep the original ordering while supporting nested damage callbacks.
    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;removeEntitiesOnShoulder()V"), cancellable = true)
    private void onHurt(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        var modifiedDamage = new MutableFloat(damage);
        polycard$modifiedDamageStack.push(modifiedDamage);
        var player = (Player) (Object) this;
        if (EntityHurtEventCallback.EVENT.invoker().onEntityHurt(player, level, source, modifiedDamage).equals(InteractionResult.FAIL)) {
            polycard$modifiedDamageStack.pop();
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;scalesWithDifficulty()Z"), argsOnly = true, name = "damage")
    private float applyModifiedDamage(float damage) {
        if (!polycard$modifiedDamageStack.isEmpty()) {
            return polycard$modifiedDamageStack.pop().floatValue();
        }
        return damage;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/world/phys/Vec3;)Ljava/lang/Object;"))
    private Object getBedRule(EnvironmentAttributeSystem instance, EnvironmentAttribute<BedRule> environmentAttribute, Vec3 vec3, Operation<Object> original) {
        return GetBedRuleEventCallback.EVENT.invoker().getBedRule((Player) (Object) this, (BedRule) original.call(instance, environmentAttribute, vec3));
    }
}
