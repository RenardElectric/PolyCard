package polycube.polycard.utils;

import net.minecraft.server.MinecraftServer;
import polycube.polycard.PolyCard;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Consumer;

/// Server-scoped tick scheduler for small one-shot and repeating tasks.
public final class TaskScheduler {
    private final Deque<ScheduledTask> tasks = new ArrayDeque<>();

    /// Schedules a one-shot task. Zero runs in the current end-of-tick pass when
    /// scheduled before this scheduler is ticked, or the next pass otherwise.
    public void runLater(int ticks, Consumer<MinecraftServer> runnable) {
        runTaskTimer(ticks, 0, runnable);
    }

    /// Schedules a task; a period of zero makes it one-shot.
    public void runTaskTimer(int delay, int period, Consumer<MinecraftServer> runnable) {
        if (delay < 0) throw new IllegalArgumentException("Scheduled-task delay cannot be negative");
        if (period < 0) throw new IllegalArgumentException("Scheduled-task period cannot be negative");
        tasks.addLast(new ScheduledTask(
                Math.addExact(delay, 1),
                period,
                Objects.requireNonNull(runnable, "runnable")
        ));
    }

    /// Runs the tasks queued before this tick began.
    public void tick(MinecraftServer server) {
        int tasksToProcess = tasks.size();
        for (int i = 0; i < tasksToProcess; i++) {
            var task = tasks.removeFirst();
            try {
                if (!task.tick(server)) {
                    tasks.addLast(task);
                } else if (task.period > 0) {
                    task.ticksLeft += task.period;
                    tasks.addLast(task);
                }
            } catch (RuntimeException exception) {
                PolyCard.LOGGER.error("Scheduled task failed and was cancelled", exception);
            }
        }
    }

    /// Discards tasks that captured state from a server which is shutting down.
    public int clear() {
        int count = tasks.size();
        tasks.clear();
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
}
