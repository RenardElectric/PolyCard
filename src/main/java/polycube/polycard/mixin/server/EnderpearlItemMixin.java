package polycube.polycard.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import polycube.polycard.events.callBacks.EnderPearlThrowEventCallback;

/// Lets card effects change the launch power of ender pearls thrown by players.
@Mixin(EnderpearlItem.class)
public class EnderpearlItemMixin {
    @WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectileFromRotation(Lnet/minecraft/world/entity/projectile/Projectile$ProjectileFactory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;FFF)Lnet/minecraft/world/entity/projectile/Projectile;"))
    private Projectile throwEnderPearl(Projectile.ProjectileFactory<?> factory, ServerLevel level, ItemStack itemStack, LivingEntity shooter,
                                       float zRotation, float power, float uncertainty, Operation<Projectile> original) {
        if (shooter instanceof ServerPlayer player) {
            power = EnderPearlThrowEventCallback.EVENT.invoker().onEnderPearlThrow(player, power);
        }
        return original.call(factory, level, itemStack, shooter, zRotation, power, uncertainty);
    }
}
