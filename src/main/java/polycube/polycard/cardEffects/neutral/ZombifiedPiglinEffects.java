package polycube.polycard.cardEffects.neutral;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import org.jspecify.annotations.Nullable;
import polycube.polycard.PolyCard;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.EntityAfterHurtEventCallback;
import polycube.polycard.events.callBacks.IsTargetedEventCallback;

public final class ZombifiedPiglinEffects extends CardEffects implements IsTargetedEventCallback, EntityAfterHurtEventCallback {
    public static final float SPAWN_REINFORCEMENTS_CHANCE = 0.25f;
    public static final int MAX_REINFORCEMENTS = 3;
    public static final int REINFORCEMENT_COOLDOWN = 20 * 30;
    private static final String REINFORCEMENT_COOLDOWN_KEY = "zombified_piglin:reinforcements";
    // Use vanilla's finite search budget for each requested reinforcement.
    private static final int MAX_SPAWN_ATTEMPTS = Zombie.REINFORCEMENT_ATTEMPTS * MAX_REINFORCEMENTS;

    @Override
    public InteractionResult onTargeted(ServerLevel level, @Nullable LivingEntity targeter, LivingEntity target, IsTargetedEventCallback.TargetingConditionsData targetingConditionsData) {
        if (target instanceof ServerPlayer player) {
            if (targeter instanceof ZombifiedPiglin) {
                if (hasCardOrRarer(player, RarityLevel.EPIC)) {
                    return InteractionResult.FAIL;
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void afterEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, float damageDealt) {
        if (entity instanceof ServerPlayer player) {
            if (source.getEntity() instanceof ServerPlayer attackingPlayer && !attackingPlayer.equals(player)) {
                if (hasCardOrRarer(player, RarityLevel.LEGENDARY)) {
                    var random = player.getRandom();
                    if (random.nextFloat() < SPAWN_REINFORCEMENTS_CHANCE
                            && level.isSpawningMonsters()
                            && cooldowns().tryStartCooldown(player, REINFORCEMENT_COOLDOWN_KEY, REINFORCEMENT_COOLDOWN)) {
                        int x = Mth.floor(player.getX());
                        int y = Mth.floor(player.getY());
                        int z = Mth.floor(player.getZ());
                        var type = EntityTypes.ZOMBIFIED_PIGLIN;
                        Zombie reinforcement = type.create(level, EntitySpawnReason.REINFORCEMENT);
                        if (reinforcement == null) return;

                        int reinforcementsSpawned = 0;
                        int spawnAttempts = 0;
                        while (reinforcementsSpawned < MAX_REINFORCEMENTS && spawnAttempts < MAX_SPAWN_ATTEMPTS) {
                            ++spawnAttempts;
                            int xt = x + Mth.nextInt(random, 0, 25) * Mth.nextInt(random, -1, 1);
                            int yt = y + Mth.nextInt(random, 0, 25) * Mth.nextInt(random, -1, 1);
                            int zt = z + Mth.nextInt(random, 0, 25) * Mth.nextInt(random, -1, 1);
                            BlockPos spawnPos = new BlockPos(xt, yt, zt);
                            if (level.hasChunk(SectionPos.blockToSectionCoord(xt), SectionPos.blockToSectionCoord(zt))
                                    && SpawnPlacements.isSpawnPositionOk(type, level, spawnPos)) {
                                reinforcement.setPos(xt, yt, zt);
                                if (level.isUnobstructed(reinforcement)
                                        && level.noCollision(reinforcement)
                                        && !level.containsAnyLiquid(reinforcement.getBoundingBox())) {
                                    reinforcement.setTarget(attackingPlayer);
                                    reinforcement.finalizeSpawn(level, level.getCurrentDifficultyAt(reinforcement.blockPosition()), EntitySpawnReason.REINFORCEMENT, null);
                                    if (level.addFreshEntity(reinforcement)) {
                                        ++reinforcementsSpawned;
                                    }
                                    reinforcement = type.create(level, EntitySpawnReason.REINFORCEMENT);
                                    if (reinforcement == null) break;
                                }
                            }
                        }

                        PolyCard.LOGGER.debug(
                                "{} has the legendary Zombified Piglin card and spawned {} of {} reinforcements against {} after {} attempts.",
                                player.getName().getString(), reinforcementsSpawned, MAX_REINFORCEMENTS,
                                attackingPlayer.getName().getString(), spawnAttempts
                        );
                    }
                }
            }
        }
    }
}
