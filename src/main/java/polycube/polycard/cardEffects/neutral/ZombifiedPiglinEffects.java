package polycube.polycard.cardEffects.neutral;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jspecify.annotations.Nullable;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.IsTargetedEventCallback;

public class ZombifiedPiglinEffects {
    public static final CardType CARD_TYPE = CardType.ZOMBIFIED_PIGLIN;

    public static final float SPAWN_REINFORCEMENTS_CHANCE = 0.25f;
    public static final int MAX_REINFORCEMENTS = 3;

    public static void register() {
        IsTargetedEventCallback.EVENT.register(ZombifiedPiglinEffects::canBeTargeted);
        EntityHurtEventCallback.EVENT.register(ZombifiedPiglinEffects::onPlayerHurt);
    }

    private static InteractionResult canBeTargeted(ServerLevel level, @Nullable LivingEntity targeter, LivingEntity target, IsTargetedEventCallback.TargetingConditionsData targetingConditionsData) {
        if (target instanceof ServerPlayer player) {
            if (targeter instanceof ZombifiedPiglin) {
                if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.EPIC)) {
                    return InteractionResult.FAIL;
                }
            }
        }

        return InteractionResult.PASS;
    }

    private static InteractionResult onPlayerHurt(LivingEntity entity, ServerLevel level, DamageSource source, MutableFloat damage) {
        if (entity instanceof ServerPlayer player) {
            if (source.getEntity() instanceof ServerPlayer attackingPlayer) {
                if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.LEGENDARY)) {
                    var random = player.getRandom();
                    if (random.nextFloat() < SPAWN_REINFORCEMENTS_CHANCE && level.isSpawningMonsters()) {
                        int x = Mth.floor(player.getX());
                        int y = Mth.floor(player.getY());
                        int z = Mth.floor(player.getZ());
                        var type = EntityType.ZOMBIFIED_PIGLIN;
                        Zombie reinforcement = type.create(level, EntitySpawnReason.REINFORCEMENT);
                        if (reinforcement == null) return InteractionResult.PASS;

                        for (int reinforcementSpawned = 0; reinforcementSpawned < MAX_REINFORCEMENTS; ) {
                            int xt = x + Mth.nextInt(random, 0, 25) * Mth.nextInt(random, -1, 1);
                            int yt = y + Mth.nextInt(random, 0, 25) * Mth.nextInt(random, -1, 1);
                            int zt = z + Mth.nextInt(random, 0, 25) * Mth.nextInt(random, -1, 1);
                            BlockPos spawnPos = new BlockPos(xt, yt, zt);
                            if (SpawnPlacements.isSpawnPositionOk(type, level, spawnPos)) {
                                reinforcement.setPos(xt, yt, zt);
                                if (level.isUnobstructed(reinforcement)
                                        && level.noCollision(reinforcement)
                                        && !level.containsAnyLiquid(reinforcement.getBoundingBox())) {
                                    reinforcement.setTarget(attackingPlayer);
                                    reinforcement.finalizeSpawn(level, level.getCurrentDifficultyAt(reinforcement.blockPosition()), EntitySpawnReason.REINFORCEMENT, null);
                                    level.addFreshEntityWithPassengers(reinforcement);
                                    ++reinforcementSpawned;
                                    reinforcement = type.create(level, EntitySpawnReason.REINFORCEMENT);
                                    if (reinforcement == null) break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }
}
