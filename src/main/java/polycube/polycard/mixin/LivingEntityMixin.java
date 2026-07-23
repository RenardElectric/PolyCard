package polycube.polycard.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import polycube.polycard.events.callBacks.EntityAfterHurtEventCallback;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.FallFlyingGliderWearEventCallback;
import polycube.polycard.utils.EffectHelpers;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/// Exposes validated non-player LivingEntity damage to card effects and writes back mutations.
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements Attackable, WaypointTransmitter {
    public LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Unique
    private final Deque<MutableFloat> polycard$modifiedDamageStack = new ArrayDeque<>();

    @WrapMethod(method = "hurtServer")
    private boolean afterHurt(ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
        var entity = (LivingEntity) (Object) this;
        if (entity instanceof Player) {
            return original.call(level, source, damage);
        }

        float effectiveHealthBefore = entity.getHealth() + entity.getAbsorptionAmount();
        boolean accepted = original.call(level, source, damage);
        if (accepted) {
            float damageDealt = Math.max(0.0F, effectiveHealthBefore - entity.getHealth() - entity.getAbsorptionAmount());
            EntityAfterHurtEventCallback.EVENT.invoker().afterEntityHurt(entity, level, source, damageDealt);
        }
        return accepted;
    }

    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"), cancellable = true)
    private void onHurt(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        var entity = (LivingEntity) (Object) this;
        if (entity instanceof Player) {
            return;
        }

        var modifiedDamage = new MutableFloat(damage);
        polycard$modifiedDamageStack.push(modifiedDamage);
        if (EntityHurtEventCallback.EVENT.invoker().onEntityHurt(entity, level, source, modifiedDamage).equals(InteractionResult.FAIL)) {
            polycard$modifiedDamageStack.pop();
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "hurtServer", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;noActionTime:I", opcode = Opcodes.PUTFIELD), argsOnly = true, name = "damage")
    private float applyModifiedDamage(float damage) {
        if (!polycard$modifiedDamageStack.isEmpty()) {
            return polycard$modifiedDamageStack.pop().floatValue();
        }
        return damage;
    }

    @ModifyExpressionValue(method = "addAdditionalSaveData", at = @At(value = "INVOKE", target = "Ljava/util/List;copyOf(Ljava/util/Collection;)Ljava/util/List;"))
    private List<MobEffectInstance> excludeTransientPersistentEffects(List<MobEffectInstance> effects) {
        return EffectHelpers.effectsForSave(effects);
    }

    @ModifyExpressionValue(method = "addAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeMap;pack()Ljava/util/List;"))
    private List<AttributeInstance.Packed> excludeTransientPersistentEffectAttributes(List<AttributeInstance.Packed> attributes) {
        var entity = (LivingEntity) (Object) this;
        return EffectHelpers.attributesForSave(attributes, entity.getActiveEffects());
    }

    @ModifyExpressionValue(method = "addAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getAbsorptionAmount()F"))
    private float excludeTransientPersistentEffectAbsorption(float absorptionAmount) {
        return EffectHelpers.externalAbsorptionAmount((LivingEntity) (Object) this, absorptionAmount);
    }

    @WrapOperation(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"))
    private void wearFallFlyingGlider(ItemStack glider, int amount, LivingEntity owner, EquipmentSlot slot, Operation<Void> original) {
        if (owner instanceof ServerPlayer player
                && FallFlyingGliderWearEventCallback.EVENT.invoker().cancelGliderWear(player, glider, slot)) {
            return;
        }
        original.call(glider, amount, owner, slot);
    }
}
