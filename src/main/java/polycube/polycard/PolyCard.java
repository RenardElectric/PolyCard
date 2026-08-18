package polycube.polycard;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import polycube.polycard.card.CardType;
import polycube.polycard.commands.*;
import polycube.polycard.data.Storage;
import polycube.polycard.events.CardItemUseEvent;
import polycube.polycard.events.CardLootEvents;
import polycube.polycard.events.callBacks.ItemUseEventCallback;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;
import polycube.polycard.events.callBacks.PlayerSecondEventCallback;
import polycube.polycard.utils.Cooldowns;
import polycube.polycard.utils.EffectHelpers;
import polycube.polycard.utils.Helpers;
import polycube.polycard.utils.TaskScheduler;

import java.util.Objects;

/// Fabric entrypoint that wires storage, commands, callbacks, loot events, and card effects.
public class PolyCard implements ModInitializer {
    public static final String MOD_ID = "polycard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static @Nullable Cooldowns cooldowns;
    private static @Nullable TaskScheduler scheduler;
    private static @Nullable Storage storage;

    /// Returns the cooldown service for the currently running server.
    public static Cooldowns cooldowns() {
        return Objects.requireNonNull(cooldowns, "PolyCard cooldowns are unavailable before the server has started");
    }

    /// Returns persistent card storage for the currently running server.
    public static Storage storage() {
        return Objects.requireNonNull(storage, "PolyCard storage is unavailable before the server has started");
    }

    /// Returns the task scheduler for the currently running server.
    public static TaskScheduler scheduler() {
        return Objects.requireNonNull(scheduler, "PolyCard scheduler is unavailable before the server has started");
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing PolyCard");
        CardType.registerEffects();
        Helpers.debug("Initialized and validated {} card type(s)", CardType.values().length);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            cooldowns = new Cooldowns();
            scheduler = new TaskScheduler();
            storage = Storage.load(server);
            scheduler.runTaskTimer(0, 20, runningServer -> {
                for (ServerPlayer player : runningServer.getPlayerList().getPlayers()) {
                    PlayerSecondEventCallback.EVENT.invoker().onPlayerSecond(runningServer, player);
                }
            });
            Helpers.debug("Initialized PolyCard state for server {}", server.getServerModName());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(_ -> {
            EffectHelpers.clearPersistentEffectState();
            int discardedTasks = Helpers.clearScheduledTasks();
            cooldowns = null;
            scheduler = null;
            storage = null;
            Helpers.debug("Cleared PolyCard server state and {} pending task(s)", discardedTasks);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            cooldowns().tick();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PlayerTickEventCallback.EVENT.invoker().onPlayerTick(server, player);
            }
            scheduler().tick(server);
            EffectHelpers.onEndServerTick();
        });

        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            return ItemUseEventCallback.EVENT.invoker().onItemUse(serverPlayer, level, hand);
        });

        PolyCardCommands.registerCommands(
                new HelpCommand(),
                new InfoCommand(),
                new EquipCommand(),
                new CombineCommand(),
                new GiveCommand(),
                new TestCommand(),
                new CooldownCommand()
        );

        new CardItemUseEvent().registerCallbacks();
        new CardLootEvents().registerCallbacks();
    }
}
