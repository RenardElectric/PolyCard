package polycube.polycard.utils;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/// A utility class that manages cooldowns for players.
/// It allows you to check if a cooldown is active, add new cooldowns, and remove existing cooldowns.
/// The cooldowns are stored in a map where the key is the player's UUID
/// and the value is another map that maps cooldown keys (strings) to Cooldown objects.
public class Cooldowns {

    private final Map<UUID, Map<String, Cooldown>> cooldowns;
    private int tickCount = 0;

    public Cooldowns() {
        cooldowns = new HashMap<>();
    }

    /// Advances the cooldown timers by a certain number of ticks and removes expired cooldowns.
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

    /// Checks if a cooldown is active for a player and a specific cooldown key.
    ///
    /// @param player      the player to check the cooldown for
    /// @param cooldownKey the key identifying the cooldown
    /// @return true if the cooldown is active, false if it is not active or does not exist
    public boolean isReady(Player player, String cooldownKey) {
        var playerCooldowns = cooldowns.get(player.getUUID());
        return playerCooldowns != null &&
                playerCooldowns.containsKey(cooldownKey) &&
                playerCooldowns.get(cooldownKey).endTime > tickCount;
    }

    /// Checks if a cooldown is active for a player and a specific cooldown key,
    /// and if not, creates a new cooldown with the specified time.
    ///
    /// @param player      the player to check the cooldown for
    /// @param cooldownKey the key identifying the cooldown
    /// @param time        the duration of the cooldown in ticks
    /// @return true if a new cooldown was created, false if the cooldown is still active
    public boolean isReadyOrCreate(Player player, String cooldownKey, int time) {
        if (isReady(player, cooldownKey)) {
            return false;
        }
        addCooldown(player, cooldownKey, time);
        return true;
    }

    /// Adds a new cooldown for a player and a specific cooldown key with the specified time.
    ///
    /// @param player      the player to add the cooldown for
    /// @param cooldownKey the key identifying the cooldown
    /// @param time        the duration of the cooldown in ticks
    public void addCooldown(Player player, String cooldownKey, int time) {
        cooldowns.computeIfAbsent(player.getUUID(), _ -> new HashMap<>()).put(cooldownKey, new Cooldown(tickCount, tickCount + time));
    }

    /// Removes an existing cooldown for a player and a specific cooldown key.
    ///
    /// @param player      the player to remove the cooldown for
    /// @param cooldownKey the key identifying the cooldown to remove
    public void removeCooldown(Player player, String cooldownKey) {
        if (cooldowns.containsKey(player.getUUID())) {
            cooldowns.get(player.getUUID()).remove(cooldownKey);
        }
    }

    private record Cooldown(int startTime, int endTime) {
    }
}
