package polycube.polycard.cardEffects.hostile;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import org.jspecify.annotations.Nullable;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.AllowPhantomSpawnEventCallback;
import polycube.polycard.events.callBacks.IsTargetedEventCallback;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;
import polycube.polycard.utils.EffectHelpers;

public class PhantomEffects extends CardEffects implements PlayerTickEventCallback, IsTargetedEventCallback, AllowPhantomSpawnEventCallback {
    public static final int SPEED_AMPLIFIER = 0;
    public static final int HASTE_AMPLIFIER = 0;
    public static final int STRENGTH_AMPLIFIER = 0;

    @Override
    public void onPlayerTick(MinecraftServer server, ServerPlayer player) {
        if (hasCardOrRarer(player, RarityLevel.COMMON)) {
            ServerStatsCounter stats = player.getStats();
            int value = Mth.clamp(stats.getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)), 1, Integer.MAX_VALUE);
            int dayLength = 24000;
            var time = player.level().getDefaultClockTime() % dayLength;
            if (value >= dayLength && time >= 13000 && time < 23000) {
                conditionsFor(player)
                        .hasCommon(() -> EffectHelpers.refreshPersistentEffect(player, MobEffects.SPEED, SPEED_AMPLIFIER))
                        .hasUncommon(() -> EffectHelpers.refreshPersistentEffect(player, MobEffects.HASTE, HASTE_AMPLIFIER))
                        .hasRare(() -> EffectHelpers.refreshPersistentEffect(player, MobEffects.STRENGTH, STRENGTH_AMPLIFIER));
            }
        }
    }

    @Override
    public InteractionResult onTargeted(ServerLevel level, @Nullable LivingEntity targeter, LivingEntity target, TargetingConditionsData targetingConditions) {
        if (target instanceof ServerPlayer player) {
            if (targeter instanceof Phantom) {
                if (hasCardOrRarer(player, RarityLevel.EPIC)) {
                    return InteractionResult.FAIL;
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean allowPhantomSpawn(ServerPlayer player, ServerLevel level) {
        return !hasCardOrRarer(player, RarityLevel.LEGENDARY);
    }
}
