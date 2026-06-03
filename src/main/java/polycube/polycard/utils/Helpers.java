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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static polycube.polycard.PolyCard.LOGGER;
import static polycube.polycard.PolyCard.MOD_ID;

/// A utility class that provides helper methods for playing sounds, scheduling tasks, and logging debug messages.
public final class Helpers {
    private static final RandomSource random = RandomSource.create();
    private static final int FAILURE_SOUND_COOLDOWN = 20;

    /// Plays a sound for a specific player at their current location.
    ///
    /// @param player the player to play the sound for
    /// @param sound  the sound event to play
    public static void playSound(ServerPlayer player, SoundEvent sound) {
        player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1.0f, 1.0f, random.nextLong()));
    }

    /// Plays a sound for a specific player with a cooldown to prevent spamming the same sound.
    ///
    /// @param player      the player to play the sound for
    /// @param sound       the sound event to play
    /// @param cooldown    the cooldown time in ticks before the sound can be played again for the same player
    public static void playSound(ServerPlayer player, SoundEvent sound, int cooldown) {
        var key = sound.toString();
        if (PolyCard.COOLDOWNS.isReadyOrCreate(player, key, cooldown)) {
            playSound(player, sound);
        }
    }

    /// Plays a failure sound for a specific player with a cooldown to prevent spamming the same sound.
    ///
    /// @param player      the player to play the failure sound for
    public static void playFailure(ServerPlayer player) {
        playSound(player, SoundEvents.VILLAGER_NO, FAILURE_SOUND_COOLDOWN);
    }

    /// Plays a sound at a specific location in the world.
    ///
    /// @param level    the server level to play the sound in
    /// @param sound    the sound event to play
    /// @param position the position to play the sound at
    public static void playSound(ServerLevel level, SoundEvent sound, Vec3 position) {
        level.playSound(null, position.x, position.y, position.z, sound, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    /// Checks if there is any other player nearby who has a card of a certain type and rarity level.
    ///
    /// @param player          the player to check around
    /// @param cardType        the type of card to check for
    /// @param rarityLevel     the rarity level of the card to check for
    /// @param distanceSquared the maximum distance squared to check for nearby players
    /// @return true if there is at least one other player nearby with the specified card, false otherwise
    public static boolean nearPlayerWithCard(ServerPlayer player, CardType cardType, RarityLevel rarityLevel, double distanceSquared) {
        //noinspection resource
        var level = player.level();
        var playerPos = player.position();
        return !level.getPlayers(p ->
                p != player &&
                        p.position().distanceToSqr(playerPos) <= distanceSquared &&
                        PlayerData.hasCardOrRarer(p, cardType, rarityLevel), 1).isEmpty();
    }

    /// Logs a debug message with the mod ID as a prefix.
    ///
    /// @param format the format string for the debug message
    /// @param args   the arguments to format into the debug message
    public static void debug(final String format, final Object... args) {
        LOGGER.debug("[" + MOD_ID + "] " + format, args);
    }

    private static final List<ScheduledTask> TASKS = new ArrayList<>();
    private static final List<BiConsumer<MinecraftServer, ServerPlayer>> PLAYER_TASKS = new ArrayList<>();

    /// Schedules a task to run after a certain number of ticks.
    ///
    /// @param ticks    the number of ticks to wait before running the task
    /// @param runnable the task to run, which accepts the Minecraft server as an argument
    public static void runLater(int ticks, Consumer<MinecraftServer> runnable) {
        runTaskTimer(ticks, 0, runnable);
    }

    /// Schedules a task to run repeatedly with a certain period after an initial delay.
    ///
    /// @param delay    the number of ticks to wait before running the task for the first time
    /// @param period   the number of ticks to wait between subsequent runs of the task (0 for no repetition)
    /// @param runnable the task to run, which accepts the Minecraft server as an argument
    public static void runTaskTimer(int delay, int period, Consumer<MinecraftServer> runnable) {
        TASKS.add(new ScheduledTask(new AtomicInteger(delay), period, runnable));
    }

    /// Schedules a task to run for each player on every server tick.
    ///
    /// @param task the task to run, which accepts the Minecraft server and the player as arguments
    public static void addPlayerTask(BiConsumer<MinecraftServer, ServerPlayer> task) {
        PLAYER_TASKS.add(task);
    }

    private record ScheduledTask(AtomicInteger ticksLeft, int period, Consumer<MinecraftServer> runnable) { }

    /// Called on every server tick to check for scheduled tasks and run them when their delay has elapsed.
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
}
