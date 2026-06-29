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
import polycube.polycard.PolyCard;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.data.PlayerData;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static polycube.polycard.PolyCard.LOGGER;
import static polycube.polycard.PolyCard.MOD_ID;

/// Shared server-side helpers for sounds, lightweight tick scheduling, and debug logging.
public final class Helpers {
    private static final RandomSource random = RandomSource.create();
    private static final int FAILURE_SOUND_COOLDOWN = 20;

    /// Plays a sound packet only for this player.
    public static void playSound(ServerPlayer player, SoundEvent sound) {
        player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1.0f, 1.0f, random.nextLong()));
    }

    /// Plays a player-local sound if its per-player cooldown has expired.
    public static void playSound(ServerPlayer player, SoundEvent sound, int cooldown) {
        var key = sound.toString();
        if (PolyCard.COOLDOWNS.isReadyOrCreate(player, key, cooldown)) {
            playSound(player, sound);
        }
    }

    /// Plays the standard failure sound with spam protection.
    public static void playFailure(ServerPlayer player) {
        playSound(player, SoundEvents.VILLAGER_NO, FAILURE_SOUND_COOLDOWN);
    }

    /// Plays a world sound at the given position.
    public static void playSound(ServerLevel level, SoundEvent sound, Vec3 position) {
        level.playSound(null, position.x, position.y, position.z, sound, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    /// Returns whether another nearby player has this card type at the requested rarity or higher.
    public static boolean nearPlayerWithCard(ServerPlayer player, CardType cardType, RarityLevel rarityLevel, double distanceSquared) {
        //noinspection resource
        var level = player.level();
        var playerPos = player.position();
        return !level.getPlayers(p ->
                p != player &&
                        p.position().distanceToSqr(playerPos) <= distanceSquared &&
                        PlayerData.hasCardOrRarer(p, cardType, rarityLevel), 1).isEmpty();
    }

    /// Logs a debug message with the mod id prefix.
    public static void debug(final String format, final Object... args) {
        LOGGER.debug("[" + MOD_ID + "] " + format, args);
    }

    private static final List<ScheduledTask> TASKS = new ArrayList<>();
    private static final List<BiConsumer<MinecraftServer, ServerPlayer>> PLAYER_TASKS = new ArrayList<>();

    /// Schedules a one-shot task on the server tick loop.
    public static void runLater(int ticks, Consumer<MinecraftServer> runnable) {
        runTaskTimer(ticks, 0, runnable);
    }

    /// Schedules a task on the server tick loop; period 0 makes it one-shot.
    public static void runTaskTimer(int delay, int period, Consumer<MinecraftServer> runnable) {
        TASKS.add(new ScheduledTask(new AtomicInteger(delay), period, runnable));
    }

    /// Registers a task that runs once per online player each server tick.
    public static void addPlayerTask(BiConsumer<MinecraftServer, ServerPlayer> task) {
        PLAYER_TASKS.add(task);
    }

    private record ScheduledTask(AtomicInteger ticksLeft, int period, Consumer<MinecraftServer> runnable) { }

    /// Runs scheduled tasks and per-player tasks for the current server tick.
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

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PLAYER_TASKS.forEach(task -> task.accept(server, player));
        }
    }

    public static String probToStr(float prob) {
        return new DecimalFormat("#.##").format(prob * 100);
    }

    public static String snakeCaseToTitleCase(String str) {
        var parts = str.split("_");
        var titleCase = new StringJoiner(" ");
        for (var part : parts) {
            if (!part.isEmpty()) {
                titleCase.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return titleCase.toString();
    }
}
