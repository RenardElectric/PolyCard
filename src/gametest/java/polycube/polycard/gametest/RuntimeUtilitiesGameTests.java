package polycube.polycard.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import polycube.polycard.utils.Cooldowns;
import polycube.polycard.utils.TaskScheduler;

import java.util.ArrayList;
import java.util.List;

public final class RuntimeUtilitiesGameTests {
    @GameTest
    public void cooldownsHonorExactTickBoundaries(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        var cooldowns = new Cooldowns();

        helper.assertTrue(cooldowns.tryStartCooldown(player, "test", 3),
                "A missing cooldown must start");
        helper.assertTrue(!cooldowns.tryStartCooldown(player, "test", 3),
                "An active cooldown must not restart");
        helper.assertTrue(cooldowns.getCooldownsForPlayer(player).get("test") == 3,
                "A new cooldown must expose its full duration");
        cooldowns.tick();
        cooldowns.tick();
        helper.assertTrue(cooldowns.isOnCooldown(player, "test")
                        && cooldowns.getCooldownsForPlayer(player).get("test") == 1,
                "The cooldown must remain active through its penultimate tick");
        cooldowns.tick();
        helper.assertTrue(!cooldowns.isOnCooldown(player, "test")
                        && cooldowns.getCooldownsForPlayer(player).isEmpty(),
                "The cooldown must expire exactly at its end tick");
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void cooldownsRejectInvalidDurationsAndCanBeRemoved(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        var cooldowns = new Cooldowns();
        try {
            cooldowns.startCooldown(player, "invalid", 0);
            helper.fail("Zero-duration cooldowns must be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
        cooldowns.startCooldown(player, "removable", 10);
        cooldowns.removeCooldown(player, "removable");
        helper.assertTrue(!cooldowns.isOnCooldown(player, "removable"),
                "Removing a cooldown must make it immediately inactive");
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void schedulerHonorsDelaysPeriodsAndQueueBoundaries(GameTestHelper helper) {
        var scheduler = new TaskScheduler();
        var events = new ArrayList<String>();

        scheduler.runLater(0, server -> {
            events.add("outer");
            scheduler.runLater(0, ignored -> events.add("nested"));
        });
        scheduler.runTaskTimer(1, 2, server -> events.add("repeat"));

        scheduler.tick(helper.getLevel().getServer());
        helper.assertTrue(events.equals(List.of("outer")),
                "Zero-delay tasks must run on the first pass while nested tasks wait");
        scheduler.tick(helper.getLevel().getServer());
        helper.assertTrue(events.equals(List.of("outer", "nested", "repeat")),
                "One-tick delay and nested zero-delay tasks must run on the second pass");
        scheduler.tick(helper.getLevel().getServer());
        helper.assertTrue(events.size() == 3, "A repeating task must wait for its full period");
        scheduler.tick(helper.getLevel().getServer());
        helper.assertTrue(events.equals(List.of("outer", "nested", "repeat", "repeat")),
                "A repeating task must run again after exactly its period");
        helper.assertTrue(scheduler.clear() == 1,
                "Clearing must report the still-scheduled repeating task");
        helper.succeed();
    }

    @GameTest
    public void schedulerCancelsFailingTasksWithoutBlockingOthers(GameTestHelper helper) {
        var scheduler = new TaskScheduler();
        var events = new ArrayList<String>();
        scheduler.runLater(0, server -> { throw new IllegalStateException("expected test failure"); });
        scheduler.runLater(0, server -> events.add("survived"));

        scheduler.tick(helper.getLevel().getServer());
        helper.assertTrue(events.equals(List.of("survived")),
                "One failing task must not block later tasks in the same pass");
        helper.assertTrue(scheduler.clear() == 0,
                "A failing one-shot task must be cancelled");
        helper.succeed();
    }
}
