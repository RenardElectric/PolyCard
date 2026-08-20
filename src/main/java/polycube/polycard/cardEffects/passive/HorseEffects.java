package polycube.polycard.cardEffects.passive;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import org.apache.commons.lang3.mutable.MutableFloat;
import polycube.polycard.PolyCard;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;
import polycube.polycard.utils.EffectHelpers;

public class HorseEffects extends CardEffects implements PlayerTickEventCallback, EntityHurtEventCallback {
    public static final float DAMAGE_IGNORED_PERCENTAGE = 0.5f;

    public static final int SPEED_EFFECT_AMPLIFIER = 2;
    public static final int JUMP_BOOST_EFFECT_AMPLIFIER = 2;
    public static final int SPEED_INCREMENT_TIME = 20 * 10;
    public static final int MAX_SPEED_BOOST = 7;

    private static final int MAX_SPEED_BOOST_TICKS = (MAX_SPEED_BOOST - SPEED_EFFECT_AMPLIFIER) * SPEED_INCREMENT_TIME;
    private final PlayerState<Integer> speedBoostTicks = new PlayerState<>();

    @Override
    public void onPlayerTick(MinecraftServer server, ServerPlayer player) {
        if (!(player.getControlledVehicle() instanceof Horse horse)) {
            speedBoostTicks.remove(player);
            return;
        }

        RarityLevel rarity = equippedRarityLevel(player);
        if (rarity == null || !rarity.isAtLeast(RarityLevel.RARE)) {
            speedBoostTicks.remove(player);
            return;
        }

        EffectHelpers.refreshPersistentEffect(horse, MobEffects.JUMP_BOOST, JUMP_BOOST_EFFECT_AMPLIFIER);

        if (!rarity.isAtLeast(RarityLevel.EPIC)) {
            speedBoostTicks.remove(player);
            return;
        }

        int speedAmplifier = SPEED_EFFECT_AMPLIFIER;
        if (rarity.isAtLeast(RarityLevel.LEGENDARY) && player.getKnownMovement().lengthSqr() > 0) {
            speedAmplifier = incrementSpeedBoost(player);
        } else {
            speedBoostTicks.remove(player);
        }

        EffectHelpers.refreshPersistentEffect(horse, MobEffects.SPEED, speedAmplifier);

    }

    private int incrementSpeedBoost(ServerPlayer player) {
        int movingTicks = speedBoostTicks.getOrDefault(player, 0);
        if (movingTicks < MAX_SPEED_BOOST_TICKS) {
            speedBoostTicks.put(player, ++movingTicks);
        }
        return SPEED_EFFECT_AMPLIFIER + movingTicks / SPEED_INCREMENT_TIME;
    }

    @Override
    public InteractionResult onEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, MutableFloat damage) {
        if (entity instanceof Horse horse && horse.getControllingPassenger() instanceof ServerPlayer player) {
            if (hasCardOrRarer(player, RarityLevel.UNCOMMON)) {
                var newDamage = damage.floatValue() * (1 - DAMAGE_IGNORED_PERCENTAGE);
                PolyCard.LOGGER.debug("{} reduced ridden-horse damage from {} to {} with an Uncommon Horse card", player.getName().getString(), damage.floatValue(), newDamage);
                damage.setValue(newDamage);
            }
        }
        return InteractionResult.PASS;
    }
}
