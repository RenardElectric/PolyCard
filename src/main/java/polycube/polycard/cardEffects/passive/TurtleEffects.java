package polycube.polycard.cardEffects.passive;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.mutable.MutableFloat;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.EntityAfterHurtEventCallback;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;
import polycube.polycard.utils.EffectHelpers;
import polycube.polycard.utils.Helpers;

public class TurtleEffects extends CardEffects implements PlayerTickEventCallback, EntityHurtEventCallback, EntityAfterHurtEventCallback {
    public static final int RESISTANCE_AMPLIFIER = 0;

    public static final float DAMAGE_DECREASE_PROBABILITY = 0.2f;

    public static final int LOW_HEALTH_THRESHOLD = 2 * 3;
    public static final int TURTLE_MASTER_DURATION = 20 * 40;
    public static final int TURTLE_MASTER_SLOWNESS_AMPLIFIER = 5;
    public static final int TURTLE_MASTER_RESISTANCE_AMPLIFIER = 3;


    @Override
    public void onPlayerTick(MinecraftServer server, ServerPlayer player) {
        if (player.isUnderWater() && hasCardOrRarer(player, RarityLevel.RARE)) {
            EffectHelpers.refreshPersistentEffect(player, MobEffects.RESISTANCE, RESISTANCE_AMPLIFIER);
        }
    }

    @Override
    public InteractionResult onEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, MutableFloat damage) {
        if (entity instanceof ServerPlayer player && hasCardOrRarer(player, RarityLevel.EPIC) && (player.isCrouching() || player.isBlocking())) {
            int damageDecrease = Helpers.binomialSelection(DAMAGE_DECREASE_PROBABILITY, damage.intValue());
            if (damageDecrease > 0) {
                damage.setValue(damage.floatValue() - damageDecrease);
                Helpers.debug("{} reduced damage by {} with an Epic Turtle card ({}% chance)", player.getName().getString(), damageDecrease, Helpers.probToStr(DAMAGE_DECREASE_PROBABILITY));
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void afterEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, float damageDealt) {
        if (entity instanceof ServerPlayer player && hasCardOrRarer(player, RarityLevel.LEGENDARY) && player.getHealth() <= LOW_HEALTH_THRESHOLD) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, TURTLE_MASTER_DURATION, TURTLE_MASTER_SLOWNESS_AMPLIFIER, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, TURTLE_MASTER_DURATION, TURTLE_MASTER_RESISTANCE_AMPLIFIER, false, true));
        }
    }
}
