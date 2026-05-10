package polycube.polycard.utils;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Cooldowns {

    private final Map<UUID, Map<String, Cooldown>> cooldowns;
    private int tickCount = 0;

    public Cooldowns() {
        cooldowns = new HashMap<>();
    }

    public void tick(int ticks) {
        tickCount += ticks;
        if (!this.cooldowns.isEmpty()) {
            for (var playerCooldowns : this.cooldowns.values()) {
                var cooldownIterator = playerCooldowns.entrySet().iterator();
                while (cooldownIterator.hasNext()) {
                    var cooldown = cooldownIterator.next().getValue();
                    if (cooldown.endTime <= tickCount) {
                        cooldownIterator.remove();
                    }
                }
            }
        }
    }

    public boolean isOnCooldown(Player player, String cooldownKey) {
        return cooldowns.containsKey(player.getUUID()) &&
                cooldowns.get(player.getUUID()).containsKey(cooldownKey) &&
                cooldowns.get(player.getUUID()).get(cooldownKey).endTime > tickCount;
    }

    public void addCooldown(Player player, String cooldownKey, int time) {
        cooldowns.computeIfAbsent(player.getUUID(), _ -> new HashMap<>()).put(cooldownKey, new Cooldown(tickCount, tickCount + time));
    }

    public void removeCooldown(Player player, String cooldownKey, int time) {
        cooldowns.computeIfAbsent(player.getUUID(), _ -> new HashMap<>()).remove(cooldownKey);
    }

    public record Cooldown(int startTime, int endTime) { }
}
