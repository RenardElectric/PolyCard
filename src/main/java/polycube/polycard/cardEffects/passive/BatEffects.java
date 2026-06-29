package polycube.polycard.cardEffects.passive;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jspecify.annotations.Nullable;
import polycube.polycard.PolyCard;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.IsTargetedEventCallback;
import polycube.polycard.utils.Helpers;

import java.util.*;

public class BatEffects {
    public static final CardType CARD_TYPE = CardType.BAT;

    public static final int NIGHT_VISION_DURATION = 220;
    public static final int INVISIBILITY_DURATION = 20 * 20;
    public static final int INVISIBILITY_COOLDOWN = 20 * 10;
    public static final int SPEED_AMPLIFIER = 30;

    private static final Map<UUID, Set<Entity>> entitesGlowing = new HashMap<>();

    public static void register() {
        Helpers.addPlayerTask((_, player) -> {
            if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.RARE)) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION, 0, true, false));

                if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.EPIC)) {
                    //noinspection resource
                    Set<Entity> entitiesToGlow = player.isCrouching() ? new HashSet<>(player.level().getEntities(player, player.getBoundingBox().inflate(1000))) : new HashSet<>();
                    Set<Entity> currentlyGlowing = new HashSet<>(entitesGlowing.getOrDefault(player.getUUID(), Collections.emptySet()));

                    for (var entity : currentlyGlowing) {
                        if (!entity.isCurrentlyGlowing() && !entitiesToGlow.contains(entity)) {
                            removeGlowing(entity, player);
                        }
                    }

                    for (var entity : entitiesToGlow) {
                        if (!entity.isCurrentlyGlowing() && !currentlyGlowing.contains(entity)) {
                            setGlowing(entity, player);
                        }
                    }

                    if (!player.isCrouching()) {
                        invisiblePlayers.remove(player.getUUID());
                        player.removeEffect(MobEffects.INVISIBILITY);
                        player.removeEffect(MobEffects.SPEED);
                    }
                }
            }
        });
        EntityHurtEventCallback.EVENT.register(BatEffects::onHurt);
        IsTargetedEventCallback.EVENT.register(BatEffects::onTargeted);
        ServerLivingEntityEvents.AFTER_DEATH.register(BatEffects::onDeath);
    }

    private static void setGlowing(Entity entity, ServerPlayer player) {
        entitesGlowing.computeIfAbsent(player.getUUID(), _ -> new HashSet<>()).add(entity);
        byte currentValue = entity.getEntityData().get(Entity.DATA_SHARED_FLAGS_ID);
        currentValue |= (1 << 6);
        List<SynchedEntityData.DataValue<?>> packedValues = List.of(SynchedEntityData.DataValue.create(Entity.DATA_SHARED_FLAGS_ID, currentValue));
        player.connection.send(new ClientboundSetEntityDataPacket(entity.getId(), packedValues));
    }

    private static void removeGlowing(Entity entity, ServerPlayer player) {
        entitesGlowing.computeIfAbsent(player.getUUID(), _ -> new HashSet<>()).remove(entity);
        byte currentValue = entity.getEntityData().get(Entity.DATA_SHARED_FLAGS_ID);
        currentValue &= ~(1 << 6);
        List<SynchedEntityData.DataValue<?>> packedValues = List.of(SynchedEntityData.DataValue.create(Entity.DATA_SHARED_FLAGS_ID, currentValue));
        player.connection.send(new ClientboundSetEntityDataPacket(entity.getId(), packedValues));
    }

    private static final Set<UUID> invisiblePlayers = new HashSet<>();

    private static InteractionResult onHurt(LivingEntity entity, ServerLevel level, DamageSource source, MutableFloat damage) {
        if (entity instanceof ServerPlayer player && PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.LEGENDARY)
                && source.getEntity() instanceof LivingEntity attacker
                && player.isCrouching()
                && PolyCard.COOLDOWNS.isReadyOrCreate(player, "bat_invisibility", INVISIBILITY_COOLDOWN)
        ) {
            var uuid = player.getUUID();
            if (invisiblePlayers.add(uuid)) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVISIBILITY_DURATION, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, INVISIBILITY_DURATION, SPEED_AMPLIFIER, false, false));
                level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1, player.getZ(), 100, 1, 1, 1, 0.01);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1, player.getZ(), 100, 1, 1, 1, 0.01);
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + 1, player.getZ(), 50, 1, 1, 1, 0.25);
                attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, INVISIBILITY_DURATION, 0, false, true));
                Helpers.runLater(INVISIBILITY_DURATION, (_) -> invisiblePlayers.remove(uuid));
            } else {
                return InteractionResult.FAIL;
            }
        }

        if (source.getDirectEntity() instanceof ServerPlayer player && invisiblePlayers.contains(player.getUUID())) {
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    private static InteractionResult onTargeted(ServerLevel level, @Nullable LivingEntity targeter, LivingEntity target, IsTargetedEventCallback.TargetingConditionsData targetingConditionsData) {
        if (target instanceof ServerPlayer player && invisiblePlayers.contains(player.getUUID())) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    private static void onDeath(LivingEntity entity, DamageSource source) {
        if (entity instanceof ServerPlayer player) {
            invisiblePlayers.remove(player.getUUID());
        }
    }
}
