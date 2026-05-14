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

    public boolean isOnCooldown(Player player, String cooldownKey) {
        var playerCooldowns = cooldowns.get(player.getUUID());
        return playerCooldowns != null &&
                playerCooldowns.containsKey(cooldownKey) &&
                playerCooldowns.get(cooldownKey).endTime > tickCount;
    }

    public void addCooldown(Player player, String cooldownKey, int time) {
        cooldowns.computeIfAbsent(player.getUUID(), _ -> new HashMap<>()).put(cooldownKey, new Cooldown(tickCount, tickCount + time));
    }

    public void removeCooldown(Player player, String cooldownKey, int time) {
        cooldowns.computeIfAbsent(player.getUUID(), _ -> new HashMap<>()).remove(cooldownKey);
    }

    public record Cooldown(int startTime, int endTime) { }
}
