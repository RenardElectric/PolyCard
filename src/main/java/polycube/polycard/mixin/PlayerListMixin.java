package polycube.polycard.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polycube.polycard.events.callBacks.PlayerLoadEventCallback;

@Mixin(ServerPlayer.class)
public abstract class PlayerListMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void firePlayerJoinEvent(MinecraftServer server, ServerLevel level, GameProfile gameProfile, ClientInformation clientInformation, CallbackInfo ci) {
        PlayerLoadEventCallback.JOIN.invoker().interact((ServerPlayer) (Object)this);
    }
}
