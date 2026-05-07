package polycube.polycard.utils;

public class Cooldown {
    private final long cooldownTime;
    private long lastUsed;

    public Cooldown(long cooldownTime) {
        this.cooldownTime = cooldownTime;
        this.lastUsed = 0;
    }

    public boolean isReady() {
        return System.currentTimeMillis() - lastUsed > cooldownTime;
    }

    public void use() {
        lastUsed = System.currentTimeMillis();
    }

    public long getRemainingTime() {
        return Math.max(0, cooldownTime - (System.currentTimeMillis() - lastUsed));
    }
}
