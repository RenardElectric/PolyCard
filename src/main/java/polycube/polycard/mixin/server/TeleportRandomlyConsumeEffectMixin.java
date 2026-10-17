package polycube.polycard.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import polycube.polycard.events.callBacks.PlayerTeleportEventCallback;

/// Notifies card effects after a consumable (chorus fruit) successfully teleported a player.
@Mixin(TeleportRandomlyConsumeEffect.class)
public class TeleportRandomlyConsumeEffectMixin {
    @WrapOperation(method = "apply", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;randomTeleport(DDDZLnet/minecraft/tags/TagKey;)Z"))
    private boolean teleportConsumer(LivingEntity entity, double x, double y, double z, boolean broadcastTeleport, TagKey<Block> forbiddenBlocks, Operation<Boolean> original) {
        if (!(entity instanceof ServerPlayer player)) {
            return original.call(entity, x, y, z, broadcastTeleport, forbiddenBlocks);
        }

        var fromLevel = player.level();
        var fromPosition = player.position();
        boolean teleported = original.call(entity, x, y, z, broadcastTeleport, forbiddenBlocks);
        if (teleported) {
            PlayerTeleportEventCallback.EVENT.invoker().onPlayerTeleport(
                    player, PlayerTeleportEventCallback.TeleportCause.CHORUS_FRUIT, fromLevel, fromPosition);
        }
        return teleported;
    }
}
