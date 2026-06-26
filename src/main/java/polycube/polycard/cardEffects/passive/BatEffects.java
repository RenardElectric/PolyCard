package polycube.polycard.cardEffects.passive;

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
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.utils.Helpers;

import java.util.*;

public class BatEffects {
    public static final CardType CARD_TYPE = CardType.BAT;

    private static final Map<UUID, Set<Entity>> entitesGlowing = new HashMap<>();

    public static void register() {
        Helpers.addPlayerTask((_, player) -> {
            if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.RARE)) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, true));

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
                }
            }
        });
        EntityHurtEventCallback.EVENT.register(BatEffects::onHurt);
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

    private static InteractionResult onHurt(LivingEntity entity, ServerLevel level, DamageSource source, MutableFloat damage) {
        return InteractionResult.PASS;
    }
}
