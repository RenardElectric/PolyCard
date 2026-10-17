package polycube.polycard.cardEffects.hostile;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import polycube.polycard.PolyCard;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.EnderPearlThrowEventCallback;
import polycube.polycard.events.callBacks.EntityAfterHurtEventCallback;
import polycube.polycard.events.callBacks.PlayerTeleportEventCallback;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;
import polycube.polycard.utils.EffectHelpers;

public final class EndermiteEffects
        extends CardEffects
        implements EnderPearlThrowEventCallback, PlayerTeleportEventCallback, EntityAfterHurtEventCallback,
        PlayerTickEventCallback, ServerLivingEntityEvents.AfterDeath {
    public static final float THROW_POWER_MULTIPLIER = 1.3f;

    public static final int SPEED_EFFECT_DURATION = 20 * 30;
    public static final int SPEED_EFFECT_AMPLIFIER = 0;

    public static final int CLEANSE_COOLDOWN = 20 * 10;

    public static final int LOW_HEALTH_THRESHOLD = 2 * 3;
    public static final int ESCAPE_TELEPORT_RANGE = 16;
    public static final int ESCAPE_COOLDOWN = 20 * 60;

    public static final int RETURN_WINDOW_DURATION = 20 * 5;
    public static final int DOUBLE_SNEAK_DELAY = 10;

    private static final int ESCAPE_TELEPORT_ATTEMPTS = 16;
    private static final int RETURN_FALLBACK_RANGE = 2;
    private static final int RETURN_TELEPORT_ATTEMPTS = 8;
    private static final int RETURN_PARTICLES_INTERVAL = 4;

    private final PlayerState<ReturnWindow> returnWindows = new PlayerState<>();

    @Override
    public float onEnderPearlThrow(ServerPlayer player, float power) {
        // A new throw replaces the previous return spot.
        returnWindows.remove(player);
        if (hasCardOrRarer(player, RarityLevel.COMMON)) {
            return power * THROW_POWER_MULTIPLIER;
        }
        return power;
    }

    @Override
    public void onPlayerTeleport(ServerPlayer player, TeleportCause cause, ServerLevel fromLevel, Vec3 fromPosition) {
        var conditions = conditionsFor(player);
        if (cause == TeleportCause.ENDER_PEARL) {
            conditions.hasUncommon(() -> player.addEffect(new MobEffectInstance(MobEffects.SPEED, SPEED_EFFECT_DURATION, SPEED_EFFECT_AMPLIFIER, false, true)));
            conditions.hasLegendary(() -> openReturnWindow(player, fromLevel, fromPosition));
        }
        conditions.hasRare(() -> removeNegativeEffect(player));
    }

    @Override
    public void afterEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, float damageDealt) {
        if (entity instanceof ServerPlayer player
                && player.isAlive()
                && damageDealt > 0
                && player.getHealth() <= LOW_HEALTH_THRESHOLD
                && hasCardOrRarer(player, RarityLevel.EPIC)
                && !cooldowns().isOnCooldown(player, "endermite_escape")
        ) {
            if (escapeTeleport(player)) {
                cooldowns().startCooldown(player, "endermite_escape", ESCAPE_COOLDOWN);
                PolyCard.LOGGER.debug("{} escaped on low health with an Epic Endermite card", player.getName().getString());
            }
        }
    }

    @Override
    public void onPlayerTick(MinecraftServer server, ServerPlayer player) {
        var window = returnWindows.get(player);
        if (window == null) {
            return;
        }

        if (player.tickCount > window.expiryTick
                || !player.level().dimension().equals(window.dimension)
                || !hasCardOrRarer(player, RarityLevel.LEGENDARY)) {
            returnWindows.remove(player);
            return;
        }

        boolean sneaking = player.isShiftKeyDown();
        if (sneaking && !window.wasSneaking) {
            if (window.lastSneakTick >= 0 && player.tickCount - window.lastSneakTick <= DOUBLE_SNEAK_DELAY) {
                returnWindows.remove(player);
                teleportBack(player, window.position);
                return;
            }
            window.lastSneakTick = player.tickCount;
        }
        window.wasSneaking = sneaking;

        if (player.tickCount % RETURN_PARTICLES_INTERVAL == 0) {
            player.level().sendParticles(ParticleTypes.PORTAL, window.position.x, window.position.y + 0.5, window.position.z, 8, 0.3, 0.6, 0.3, 0.3);
        }
    }

    @Override
    public void afterDeath(LivingEntity entity, DamageSource source) {
        if (entity instanceof ServerPlayer player) {
            returnWindows.remove(player);
        }
    }

    private void openReturnWindow(ServerPlayer player, ServerLevel fromLevel, Vec3 fromPosition) {
        // Returning across dimensions is not supported.
        if (fromLevel != player.level()) {
            return;
        }
        returnWindows.put(player, new ReturnWindow(fromLevel.dimension(), fromPosition, player.tickCount + RETURN_WINDOW_DURATION, player.isShiftKeyDown()));
        player.sendOverlayMessage(Component.literal("Sneak twice to return"));
    }

    private void removeNegativeEffect(ServerPlayer player) {
        // Card-managed persistent effects would be reapplied on the next tick, so they are never picked.
        var negativeEffects = player.getActiveEffects().stream()
                .filter(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                .filter(effect -> EffectHelpers.persistentEffectOwner(effect) == null)
                .toList();
        if (negativeEffects.isEmpty() || !cooldowns().tryStartCooldown(player, "endermite_cleanse", CLEANSE_COOLDOWN)) {
            return;
        }

        var removedEffect = negativeEffects.get(player.getRandom().nextInt(negativeEffects.size()));
        player.removeEffect(removedEffect.getEffect());
        PolyCard.LOGGER.debug("{} removed {} by teleporting with a Rare Endermite card", player.getName().getString(), removedEffect.getEffect().getRegisteredName());
    }

    private boolean escapeTeleport(ServerPlayer player) {
        var level = player.level();
        var random = player.getRandom();
        for (int i = 0; i < ESCAPE_TELEPORT_ATTEMPTS; i++) {
            double x = player.getX() + (random.nextDouble() - 0.5) * 2 * ESCAPE_TELEPORT_RANGE;
            double y = Mth.clamp(player.getY() + random.nextInt(2 * ESCAPE_TELEPORT_RANGE) - ESCAPE_TELEPORT_RANGE, level.getMinY(), level.getMinY() + level.getLogicalHeight() - 1);
            double z = player.getZ() + (random.nextDouble() - 0.5) * 2 * ESCAPE_TELEPORT_RANGE;
            if (cardTeleport(player, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    private void teleportBack(ServerPlayer player, Vec3 position) {
        if (cardTeleport(player, position.x, position.y, position.z)) {
            return;
        }

        var random = player.getRandom();
        for (int i = 0; i < RETURN_TELEPORT_ATTEMPTS; i++) {
            double x = position.x + (random.nextDouble() - 0.5) * 2 * RETURN_FALLBACK_RANGE;
            double y = position.y + random.nextInt(2 * RETURN_FALLBACK_RANGE + 1) - RETURN_FALLBACK_RANGE;
            double z = position.z + (random.nextDouble() - 0.5) * 2 * RETURN_FALLBACK_RANGE;
            if (cardTeleport(player, x, y, z)) {
                return;
            }
        }
        player.sendOverlayMessage(Component.literal("Your return spot is blocked"));
    }

    /// Teleports the player to the first safe ground at or below the target, like a chorus fruit.
    private boolean cardTeleport(ServerPlayer player, double x, double y, double z) {
        var fromLevel = player.level();
        var fromPosition = player.position();
        if (player.isPassenger()) {
            player.stopRiding();
        }
        if (!player.randomTeleport(x, y, z, true, BlockTags.CONSUMABLE_DOES_NOT_TELEPORT_TO)) {
            return false;
        }

        player.resetFallDistance();
        fromLevel.playSound(null, fromPosition.x, fromPosition.y, fromPosition.z, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS);
        fromLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS);
        PlayerTeleportEventCallback.EVENT.invoker().onPlayerTeleport(player, TeleportCause.CARD_EFFECT, fromLevel, fromPosition);
        return true;
    }

    private static final class ReturnWindow {
        private final ResourceKey<Level> dimension;
        private final Vec3 position;
        private final int expiryTick;
        private boolean wasSneaking;
        private int lastSneakTick = -1;

        private ReturnWindow(ResourceKey<Level> dimension, Vec3 position, int expiryTick, boolean wasSneaking) {
            this.dimension = dimension;
            this.position = position;
            this.expiryTick = expiryTick;
            this.wasSneaking = wasSneaking;
        }
    }
}
