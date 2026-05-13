package polycube.polycard;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import polycube.polycard.commands.*;
import polycube.polycard.events.callBacks.ItemUseEventCallback;
import polycube.polycard.events.cardDropEvents.CardDropEvents;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.guiEvents.CardItemUseEvent;
import polycube.polycard.gui.EquipmentGUI;
import polycube.polycard.manager.CardManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class PolyCard implements ModInitializer {
    public static final String MOD_ID = "polycard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final List<ScheduledTask> TASKS = new ArrayList<>();

    @Override
    public void onInitialize() {
        debug("PolyCard mod initialized.");
        CardManager cardManager = new CardManager();
        EquipmentGUI equipmentGUI = new EquipmentGUI(cardManager);

        ServerLifecycleEvents.SERVER_STARTED.register(cardManager::load);
        ServerTickEvents.END_SERVER_TICK.register(PolyCard::onServerTick);

        PolyCardCommands.registerCommands(
                new HelpCommand(),
                new GiveCommand(),
                new TestCommand(),
                new EquipCommand(equipmentGUI),
                new InfoCommand()
        );

        ItemUseEventCallback.register(new CardItemUseEvent(cardManager));

        CardDropEvents.registerCardDropEvents();
        CardEffects.registerCardEffects(cardManager);
    }

    // Small helpers
    private static final RandomSource random = RandomSource.create();
    public static void playSound(ServerPlayer player, SoundEvent sound) {
        player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1.0f, 1.0f, random.nextLong()));
    }

    public static void debug(final String format, final Object... args) {
        LOGGER.debug("[" + MOD_ID + "] " + format, args);
    }

    public static void runLater(int ticks, Consumer<MinecraftServer> runnable) {
        runTaskTimer(ticks, 0, runnable);
    }

    public static void runTaskTimer(int delay, int period, Consumer<MinecraftServer> runnable) {
        TASKS.add(new ScheduledTask(new AtomicInteger(delay), period, runnable));
    }

    private record ScheduledTask(AtomicInteger ticksLeft, int period, Consumer<MinecraftServer> runnable) { }

    private static void onServerTick(MinecraftServer server) {
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