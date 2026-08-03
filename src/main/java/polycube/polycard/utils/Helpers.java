package polycube.polycard.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import polycube.polycard.PolyCard;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.function.Consumer;

import static polycube.polycard.PolyCard.LOGGER;
import static polycube.polycard.PolyCard.MOD_ID;

/// Shared server-side helpers for sounds, lightweight tick scheduling, and debug logging.
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

    /// Plays the standard failure sound with spam protection.
    public static void SendFailure(ServerPlayer player, MutableComponent reason) {
        var key = "sound:" + BuiltInRegistries.SOUND_EVENT.getKey(SoundEvents.VILLAGER_NO);
        if (PolyCard.cooldowns().tryStartCooldown(player, key, FAILURE_COOLDOWN)) {
            playSound(player, SoundEvents.VILLAGER_NO);
            player.sendSystemMessage(reason.withStyle(ChatFormatting.RED));
        }
    }

    /// Plays a world sound at the given position.
    public static void playSound(ServerLevel level, SoundEvent sound, Vec3 position) {
        level.playSound(null, position.x, position.y, position.z, sound, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    /// Logs a debug message with the mod id prefix.
    public static void debug(final String format, final Object... args) {
        //noinspection StringConcatenationArgumentToLogCall
        LOGGER.debug("[" + MOD_ID + "] " + format, args);
    }

    private static final Deque<ScheduledTask> TASKS = new ArrayDeque<>();

    /// Schedules a one-shot task on the server tick loop. Zero runs in the current end-of-tick pass
    /// and a positive delay waits that many complete ticks.
    public static void runLater(int ticks, Consumer<MinecraftServer> runnable) {
        runTaskTimer(ticks, 0, runnable);
    }

    /// Schedules a task on the server tick loop; period 0 makes it one-shot.
    public static void runTaskTimer(int delay, int period, Consumer<MinecraftServer> runnable) {
        if (delay < 0) {
            throw new IllegalArgumentException("Scheduled-task delay cannot be negative");
        }
        TASKS.add(new ScheduledTask(Math.addExact(delay, 1), period, Objects.requireNonNull(runnable, "runnable")));
    }

    /// Discards tasks that captured state from a server which is shutting down.
    public static int clearScheduledTasks() {
        int count = TASKS.size();
        TASKS.clear();
        return count;
    }

    private static final class ScheduledTask {
        private int ticksLeft;
        private final int period;
        private final Consumer<MinecraftServer> runnable;

        private ScheduledTask(int ticksLeft, int period, Consumer<MinecraftServer> runnable) {
            this.ticksLeft = ticksLeft;
            this.period = period;
            this.runnable = runnable;
        }

        private boolean tick(MinecraftServer server) {
            if (--ticksLeft > 0) {
                return false;
            }
            runnable.accept(server);
            return true;
        }
    }


    /// Runs scheduled tasks and per-player tasks for the current server tick.
    public static void onServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerTickEventCallback.EVENT.invoker().onPlayerTick(server, player);
        }

        int tasksToProcess = TASKS.size();
        for (int i = 0; i < tasksToProcess; i++) {
            var task = TASKS.removeFirst();
            try {
                if (!task.tick(server)) {
                    TASKS.addLast(task);
                } else if (task.period > 0) {
                    task.ticksLeft += task.period;
                    TASKS.addLast(task);
                }
            } catch (RuntimeException exception) {
                LOGGER.error("Scheduled PolyCard task failed", exception);
            }
        }

        EffectHelpers.onEndServerTick();
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
