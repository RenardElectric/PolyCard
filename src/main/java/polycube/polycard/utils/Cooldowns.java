package polycube.polycard.utils;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/// Tick-based cooldowns keyed by player UUID and effect-specific string keys.
public class Cooldowns {

    private final Map<UUID, Map<String, Cooldown>> cooldowns;
    private int tickCount = 0;

    public Cooldowns() {
        cooldowns = new HashMap<>();
    }

    /// Advances time by one tick and removes expired cooldown entries.
    public void tick() {
        ++tickCount;
        if (!this.cooldowns.isEmpty()) {
            var playerIterator = this.cooldowns.entrySet().iterator();
            while (playerIterator.hasNext()) {
                var playerCooldowns = playerIterator.next().getValue();
                var cooldownIterator = playerCooldowns.entrySet().iterator();
                while (cooldownIterator.hasNext()) {
                    var cooldown = cooldownIterator.next().getValue();
                    if (cooldown.endTime <= tickCount) {
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
    public boolean isReady(Player player, String cooldownKey) {
        var playerCooldowns = cooldowns.get(player.getUUID());
        return playerCooldowns != null &&
                playerCooldowns.containsKey(cooldownKey) &&
                playerCooldowns.get(cooldownKey).endTime > tickCount;
    }

    /// Starts a cooldown and returns true only when no active cooldown already existed.
    public boolean isReadyOrCreate(Player player, String cooldownKey, int time) {
        if (isReady(player, cooldownKey)) {
            return false;
        }
        addCooldown(player, cooldownKey, time);
        return true;
    }

    /// Adds or replaces a cooldown measured in server ticks.
    public void addCooldown(Player player, String cooldownKey, int time) {
        cooldowns.computeIfAbsent(player.getUUID(), _ -> new HashMap<>()).put(cooldownKey, new Cooldown(tickCount, tickCount + time));
    }

    /// Clears a cooldown key for this player.
    public void removeCooldown(Player player, String cooldownKey) {
        if (cooldowns.containsKey(player.getUUID())) {
            cooldowns.get(player.getUUID()).remove(cooldownKey);
        }
    }

    public Map<String, Integer> getCooldownsForPlayer(Player player) {
        return cooldowns.getOrDefault(player.getUUID(), new HashMap<>())
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().endTime > tickCount)
                .collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue().endTime - tickCount), HashMap::putAll);
    }

    public record Cooldown(int startTime, int endTime) { }
}
