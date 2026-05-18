package polycube.polycard.utils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import polycube.polycard.manager.CardManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static polycube.polycard.PolyCard.LOGGER;
import static polycube.polycard.PolyCard.MOD_ID;

public final class Helpers {
    private static final RandomSource random = RandomSource.create();
    private static final int FAILURE_SOUND_COOLDOWN = 20;

    public static void playSound(ServerPlayer player, SoundEvent sound) {
        player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1.0f, 1.0f, random.nextLong()));
    }

    public static void playSound(CardManager cardManager, ServerPlayer player, SoundEvent sound, int cooldown) {
        var key = sound.toString();
        if (cardManager.getCooldowns().isReadyOrCreate(player, key, cooldown)) {
            playSound(player, sound);
        }
    }

    public static void playFailure(CardManager cardManager, ServerPlayer player) {
        playSound(cardManager, player, SoundEvents.VILLAGER_NO, FAILURE_SOUND_COOLDOWN);
    }

    public static void playSound(MinecraftServer server, SoundEvent sound) {
        server.getPlayerList().getPlayers().forEach(player -> playSound(player, sound));
    }

    public static void playSound(ServerLevel level, SoundEvent sound, Vec3 position) {
        level.playSound(null, position.x, position.y, position.z, sound, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    public static void debug(final String format, final Object... args) {
        LOGGER.debug("[" + MOD_ID + "] " + format, args);
    }

    private static final List<ScheduledTask> TASKS = new ArrayList<>();
    public static void runLater(int ticks, Consumer<MinecraftServer> runnable) {
        runTaskTimer(ticks, 0, runnable);
    }

    public static void runTaskTimer(int delay, int period, Consumer<MinecraftServer> runnable) {
        TASKS.add(new ScheduledTask(new AtomicInteger(delay), period, runnable));
    }

    private record ScheduledTask(AtomicInteger ticksLeft, int period, Consumer<MinecraftServer> runnable) { }

    public static void onServerTick(MinecraftServer server) {
        var iterator = TASKS.iterator();
        while (iterator.hasNext()) {
            ScheduledTask task = iterator.next();
            if (task.ticksLeft.decrementAndGet() <= 0) {
                task.runnable.accept(server);
                if (task.period > 0) {
                    task.ticksLeft.set(task.period);
                } else {
                    iterator.remove();
                }
            }
        }
    }
}
