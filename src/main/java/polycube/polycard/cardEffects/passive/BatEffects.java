package polycube.polycard.cardEffects.passive;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jspecify.annotations.Nullable;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.CardEventCallback;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.IsTargetedEventCallback;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;
import polycube.polycard.utils.EffectHelpers;
import polycube.polycard.utils.Helpers;

import java.util.*;

public class BatEffects
        extends CardEffects
        implements PlayerTickEventCallback, EntityHurtEventCallback,
        IsTargetedEventCallback, ServerLivingEntityEvents.AfterDeath,
        CardEventCallback.CardUnequipEvent {
    public static final int INVISIBILITY_DURATION = 20 * 20;
    public static final int INVISIBILITY_COOLDOWN = 20 * 10;
    public static final int SPEED_AMPLIFIER = 30;
    public static final double GLOW_RANGE = 32.0;

    private static final int GLOW_UPDATE_INTERVAL = 10;
    private static final int GLOWING_FLAG = 1 << 6;
    private static final Identifier SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, "bat_invisibility_speed");
    private static final AttributeModifier SPEED_MODIFIER = new AttributeModifier(
            SPEED_MODIFIER_ID,
            0.2D * (SPEED_AMPLIFIER + 1),
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    private final PlayerState<Set<Integer>> glowingEntityIds = new PlayerState<>();
    private final PlayerState<Long> invisiblePlayers = new PlayerState<>();
    private long nextInvisibilityId;

    @Override
    public void onPlayerTick(MinecraftServer server, ServerPlayer player) {
        var equippedRarity = equippedRarityLevel(player);
        boolean hasRare = equippedRarity != null && equippedRarity.isAtLeast(RarityLevel.RARE);
        boolean hasEpic = equippedRarity != null && equippedRarity.isAtLeast(RarityLevel.EPIC);
        boolean hasLegendary = equippedRarity != null && equippedRarity.isAtLeast(RarityLevel.LEGENDARY);

        if (hasRare) {
            EffectHelpers.refreshPersistentEffect(player, MobEffects.NIGHT_VISION, 0);
        }

        if (hasEpic && player.isCrouching()) {
            if (player.tickCount % GLOW_UPDATE_INTERVAL == 0) {
                updateGlowingEntities(player);
            }
        } else {
            clearGlowingEntities(player);
        }

        var invisibilityId = invisiblePlayers.get(player);
        if (invisibilityId != null && (!hasLegendary || !player.isCrouching())) {
            endInvisibility(player);
        }
    }

    private void updateGlowingEntities(ServerPlayer player) {
        var level = player.level();
        var nearbyEntities = level.getEntities(
                player,
                player.getBoundingBox().inflate(GLOW_RANGE),
                entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator()
        );
        var desiredEntities = new HashMap<Integer, Entity>();
        for (var entity : nearbyEntities) {
            if (!entity.isCurrentlyGlowing()) {
                desiredEntities.put(entity.getId(), entity);
            }
        }

        var currentIds = glowingEntityIds.get(player);
        if (currentIds == null) {
            currentIds = new HashSet<>();
            glowingEntityIds.put(player, currentIds);
        }
        for (var entityId : new HashSet<>(currentIds)) {
            if (!desiredEntities.containsKey(entityId)) {
                var entity = level.getEntity(entityId);
                if (entity != null) {
                    sendEntityFlags(player, entity, false);
                }
                currentIds.remove(entityId);
            }
        }
        for (var entry : desiredEntities.entrySet()) {
            currentIds.add(entry.getKey());
            sendEntityFlags(player, entry.getValue(), true);
        }

        if (currentIds.isEmpty()) {
            glowingEntityIds.remove(player);
        }
    }

    private void clearGlowingEntities(ServerPlayer player) {
        var entityIds = glowingEntityIds.remove(player);
        if (entityIds == null) {
            return;
        }
        var level = player.level();
        for (var entityId : entityIds) {
            var entity = level.getEntity(entityId);
            if (entity != null) {
                sendEntityFlags(player, entity, false);
            }
        }
    }

    private static void sendEntityFlags(ServerPlayer viewer, Entity entity, boolean forceGlowing) {
        byte flags = entity.getEntityData().get(Entity.DATA_SHARED_FLAGS_ID);
        if (forceGlowing) {
            flags |= GLOWING_FLAG;
        }
        List<SynchedEntityData.DataValue<?>> packedValues = List.of(
                SynchedEntityData.DataValue.create(Entity.DATA_SHARED_FLAGS_ID, flags)
        );
        viewer.connection.send(new ClientboundSetEntityDataPacket(entity.getId(), packedValues));
    }

    @Override
    public InteractionResult onEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, MutableFloat damage) {
        if (entity instanceof ServerPlayer player
                && !invisiblePlayers.contains(player)
                && hasCardOrRarer(player, RarityLevel.LEGENDARY)
                && source.getEntity() instanceof LivingEntity attacker
                && player.isCrouching()
                && cooldowns().tryStartCooldown(player, "bat_invisibility", INVISIBILITY_COOLDOWN)
        ) {
            startInvisibility(player, attacker, level);
        }

        // The triggering hit and all later incoming damage are canceled while the state is active.
        if (entity instanceof ServerPlayer player && invisiblePlayers.contains(player)) {
            return InteractionResult.FAIL;
        }

        // Block melee and player-owned projectile damage dealt by an active Bat player.
        if (source.getEntity() instanceof ServerPlayer player && invisiblePlayers.contains(player)) {
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onTargeted(ServerLevel level, @Nullable LivingEntity targeter, LivingEntity target, IsTargetedEventCallback.TargetingConditionsData data) {
        if (target instanceof ServerPlayer player && invisiblePlayers.contains(player)) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onCardUnequip(ServerPlayer player, Card card) {
        if (card.cardType() == cardType()) {
            clearGlowingEntities(player);
            endInvisibility(player);
        }
    }

    @Override
    protected void onPlayerStateClearing(ServerPlayer player) {
        glowingEntityIds.remove(player);
        endInvisibility(player);
    }

    @Override
    protected void onRuntimeClearing() {
        nextInvisibilityId = 0;
    }

    @Override
    public void afterDeath(LivingEntity entity, DamageSource source) {
        if (entity instanceof ServerPlayer player) {
            clearGlowingEntities(player);
            endInvisibility(player);
        }
    }

    private void startInvisibility(ServerPlayer player, LivingEntity attacker, ServerLevel level) {
        long invisibilityId = ++nextInvisibilityId;

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVISIBILITY_DURATION, 0, false, false));

        var movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(SPEED_MODIFIER_ID);
            movementSpeed.addTransientModifier(SPEED_MODIFIER);
        }

        invisiblePlayers.put(player, invisibilityId);
        scheduler().runLater(INVISIBILITY_DURATION, _ -> endInvisibility(player, invisibilityId));

        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1, player.getZ(), 100, 1, 1, 1, 0.01);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1, player.getZ(), 100, 1, 1, 1, 0.01);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + 1, player.getZ(), 50, 1, 1, 1, 0.25);
        attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, INVISIBILITY_DURATION, 0, false, true));
        Helpers.debug("Activated legendary Bat state for {}", player.getName().getString());
    }

    private void endInvisibility(ServerPlayer player, long expectedInvisibilityId) {
        var invisibilityId = invisiblePlayers.get(player);
        if (invisibilityId != null && invisibilityId == expectedInvisibilityId) {
            endInvisibility(player);
        }
    }

    private void endInvisibility(ServerPlayer player) {
        var invisibility = invisiblePlayers.remove(player);
        if (invisibility == null) {
            return;
        }

        var movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(SPEED_MODIFIER_ID);
        }

        player.removeEffect(MobEffects.INVISIBILITY);
        Helpers.debug("Ended legendary Bat state for {}", player.getName().getString());
    }
}
