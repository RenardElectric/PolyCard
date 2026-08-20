package polycube.polycard.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import polycube.polycard.PolyCard;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/// Shared server-side helpers for sounds, formatting, and random selection.
public final class Helpers {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final int FAILURE_COOLDOWN = 20;
    private static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT = ThreadLocal.withInitial(
            () -> new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT))
    );

    private Helpers() {}

    /// Plays a sound packet only for this player.
    public static void playSound(ServerPlayer player, SoundEvent sound) {
        player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1.0f, 1.0f, RANDOM.nextLong()));
    }

    /// Sends a success message
    public static void SendSuccess(ServerPlayer player, MutableComponent message) {
        player.sendSystemMessage(message.withStyle(ChatFormatting.GREEN));
    }

    /// Sends a failure with spam protection.
    public static void SendFailure(ServerPlayer player, MutableComponent reason) {
        var key = "sound:" + BuiltInRegistries.SOUND_EVENT.getKey(SoundEvents.VILLAGER_NO);
        if (PolyCard.runtime().cooldowns().tryStartCooldown(player, key, FAILURE_COOLDOWN)) {
            playSound(player, SoundEvents.VILLAGER_NO);
            player.sendSystemMessage(reason.withStyle(ChatFormatting.RED));
        }
    }

    /// Plays a world sound at the given position.
    public static void playSound(ServerLevel level, SoundEvent sound, Vec3 position) {
        level.playSound(null, position.x, position.y, position.z, sound, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    public static String decimalFormat(double input) {
        return DECIMAL_FORMAT.get().format(input);
    }

    public static String probToStr(double prob) {
        return decimalFormat(prob * 100);
    }

    /// Turns namespaced/snake-case keys into readable command output.
    public static String identifierToTitleCase(String str) {
        var parts = str.split("[_:./]+");
        var titleCase = new StringJoiner(" ");
        for (var part : parts) {
            if (!part.isEmpty()) {
                titleCase.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return titleCase.toString();
    }

    public static int binomialSelection(float probability, int count) {
        int result = 0;
        for (int i = 0; i < count; i++) {
            if (RANDOM.nextFloat() < probability) result++;
        }
        return result;
    }
}
