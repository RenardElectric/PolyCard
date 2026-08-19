package polycube.polycard;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.data.Storage;
import polycube.polycard.events.callBacks.PlayerSecondEventCallback;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;
import polycube.polycard.utils.Cooldowns;
import polycube.polycard.utils.EffectHelpers;
import polycube.polycard.utils.TaskScheduler;

/// Owns all mutable state whose lifetime is exactly one running Minecraft server.
public final class PolyCardRuntime {
    private final Cooldowns cooldowns;
    private final TaskScheduler scheduler;
    private final Storage storage;
    private boolean closed;

    private PolyCardRuntime(Storage storage) {
        this.cooldowns = new Cooldowns();
        this.scheduler = new TaskScheduler();
        this.storage = storage;
    }

    /// Creates a fully initialized runtime without publishing partial state to callers.
    public static PolyCardRuntime start(MinecraftServer server) {
        var runtime = new PolyCardRuntime(Storage.load(server));
        runtime.scheduler.runTaskTimer(0, 20, runningServer -> {
            for (ServerPlayer player : runningServer.getPlayerList().getPlayers()) {
                PlayerSecondEventCallback.EVENT.invoker().onPlayerSecond(runningServer, player);
            }
        });
        return runtime;
    }

    /// Advances every server-scoped subsystem in its required order.
    public void tick(MinecraftServer server) {
        ensureOpen();
        cooldowns.tick();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerTickEventCallback.EVENT.invoker().onPlayerTick(server, player);
        }
        scheduler.tick(server);
        EffectHelpers.onEndServerTick();
    }

    /// Closes this runtime once and returns the number of tasks discarded during shutdown.
    public int close() {
        if (closed) {
            return 0;
        }
        closed = true;
        int discardedTasks = scheduler.clear();
        EffectHelpers.clearPersistentEffectState();
        CardEffects.clearRuntimeState();
        return discardedTasks;
    }

    public Cooldowns cooldowns() {
        ensureOpen();
        return cooldowns;
    }

    public TaskScheduler scheduler() {
        ensureOpen();
        return scheduler;
    }

    public Storage storage() {
        ensureOpen();
        return storage;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("PolyCard runtime is already closed");
        }
    }
}
