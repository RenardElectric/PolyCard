package polycube.polycard.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import polycube.polycard.events.callBacks.PlayerTeleportEventCallback;

/// Notifies card effects after an ender pearl successfully teleported its player owner.
@Mixin(ThrownEnderpearl.class)
public class ThrownEnderpearlMixin {
    @WrapOperation(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;"))
    private ServerPlayer teleportOwner(ServerPlayer player, TeleportTransition transition, Operation<ServerPlayer> original) {
        var fromLevel = player.level();
        var fromPosition = player.position();
        var teleportedPlayer = original.call(player, transition);
        if (teleportedPlayer != null) {
            PlayerTeleportEventCallback.EVENT.invoker().onPlayerTeleport(
                    teleportedPlayer, PlayerTeleportEventCallback.TeleportCause.ENDER_PEARL, fromLevel, fromPosition);
        }
        return teleportedPlayer;
    }
}
