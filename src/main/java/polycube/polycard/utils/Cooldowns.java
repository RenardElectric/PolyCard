package polycube.polycard.utils;

import net.minecraft.world.entity.player.Player;

import java.util.*;

/// Tick-based cooldowns keyed by player UUID and effect-specific string keys.
public class Cooldowns {
    private static final int CLEANUP_INTERVAL_TICKS = 20;

    private final Map<UUID, Map<String, Cooldown>> cooldowns;
    private long tickCount = 0;

    public Cooldowns() {
        cooldowns = new HashMap<>();
    }

    /// Advances time by one tick and removes expired cooldown entries.
    public void tick() {
        ++tickCount;
        // Read operations already ignore expired entries. Sweeping once per second avoids walking
        // every active player's cooldown map on every server tick.
        if (tickCount % CLEANUP_INTERVAL_TICKS == 0 && !this.cooldowns.isEmpty()) {
            var playerIterator = this.cooldowns.entrySet().iterator();
            while (playerIterator.hasNext()) {
                var playerCooldowns = playerIterator.next().getValue();
                var cooldownIterator = playerCooldowns.entrySet().iterator();
                while (cooldownIterator.hasNext()) {
                    var cooldown = cooldownIterator.next().getValue();
                    if (cooldown.endTick <= tickCount) {
                        cooldownIterator.remove();
                    }
                }
                if (playerCooldowns.isEmpty()) {
                    playerIterator.remove();
                }
            }
        }
    }

    /// Returns true while the cooldown is still active.
    public boolean isOnCooldown(Player player, String cooldownKey) {
        var playerCooldowns = cooldowns.get(player.getUUID());
        if (playerCooldowns != null) {
            var playerCooldown = playerCooldowns.get(cooldownKey);
            return playerCooldown != null && playerCooldown.endTick > tickCount;
        }
        return false;
    }

    /// Starts a cooldown and returns true only when no active cooldown already existed.
    public boolean tryStartCooldown(Player player, String cooldownKey, int durationTicks) {
        if (isOnCooldown(player, cooldownKey)) {
            return false;
        }
        startCooldown(player, cooldownKey, durationTicks);
        return true;
    }

    /// Adds or replaces a cooldown measured in server ticks.
    public void startCooldown(Player player, String cooldownKey, int durationTicks) {
        if (durationTicks <= 0) {
            throw new IllegalArgumentException("Cooldown duration must be positive");
        }
        cooldowns.computeIfAbsent(player.getUUID(), _ -> new HashMap<>())
                .put(cooldownKey, new Cooldown(tickCount + durationTicks));
    }

    /// Clears a cooldown key for this player.
    @SuppressWarnings("unused")
    public void removeCooldown(Player player, String cooldownKey) {
        var playerCooldowns = cooldowns.get(player.getUUID());
        if (playerCooldowns != null) {
            playerCooldowns.remove(cooldownKey);
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(player.getUUID());
            }
        }
    }

    /// Returns an immutable, alphabetically ordered snapshot of active cooldowns and remaining ticks.
    public Map<String, Integer> getCooldownsForPlayer(Player player) {
        var result = new TreeMap<String, Integer>();
        cooldowns.getOrDefault(player.getUUID(), Map.of()).forEach((key, cooldown) -> {
            long remainingTicks = cooldown.endTick - tickCount;
            if (remainingTicks > 0) {
                result.put(key, Math.toIntExact(remainingTicks));
            }
        });
        return Collections.unmodifiableMap(result);
    }

    private record Cooldown(long endTick) {
    }
}
