package polycube.polycard.cardEffects.passive;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import org.apache.commons.lang3.mutable.MutableFloat;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;
import polycube.polycard.utils.CardRarityConditions;
import polycube.polycard.utils.Helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HorseEffects extends CardEffects implements PlayerTickEventCallback, EntityHurtEventCallback {
    public static final float DAMAGE_IGNORED_PERCENTAGE = 0.5f;

    public static final int SPEED_EFFECT_AMPLIFIER = 2;
    public static final int JUMP_BOOST_EFFECT_AMPLIFIER = 2;
    public static final int SPEED_INCREMENT_TIME = 20 * 10;
    public static final int MAX_SPEED_BOOST = 7;

    private static final Map<UUID, Integer> speedBoostMap = new HashMap<>();

    @Override
    public void onPlayerTick(MinecraftServer server, ServerPlayer player) {
        if (player.getControlledVehicle() instanceof Horse horse) {
            CardRarityConditions.of(player, cardType)
                .hasRare(() -> horse.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 2, JUMP_BOOST_EFFECT_AMPLIFIER, true, false)))
                .hasEpic(() -> horse.addEffect(new MobEffectInstance(MobEffects.SPEED, 2, SPEED_EFFECT_AMPLIFIER, true, false)))
                .hasLegendary(() -> {
                    if (player.getKnownMovement().lengthSqr() > 0) {
                        int timeSprinting = speedBoostMap.getOrDefault(player.getUUID(), 0) + 1;
                        speedBoostMap.put(player.getUUID(), timeSprinting);
                        int speed = Math.min(SPEED_EFFECT_AMPLIFIER + timeSprinting / SPEED_INCREMENT_TIME, MAX_SPEED_BOOST);
                        horse.addEffect(new MobEffectInstance(MobEffects.SPEED, 2, speed, true, false));
                    } else {
                        speedBoostMap.remove(player.getUUID());
                    }
                }, () -> speedBoostMap.remove(player.getUUID()));
        }
    }

    @Override
    public InteractionResult onEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, MutableFloat damage) {
        if (entity instanceof Horse horse && horse.getControllingPassenger() instanceof ServerPlayer player) {
            if (PlayerData.hasCardOrRarer(player, cardType, RarityLevel.UNCOMMON)) {
                var newDamage = damage.floatValue() * (1 - DAMAGE_IGNORED_PERCENTAGE);
                Helpers.debug("{} has the uncommon horse card and is riding a horse. Reducing damage taken by from {} to {}.", player.getName().getString(), damage.floatValue(), newDamage);
                damage.setValue(newDamage);
            }
        }
        return InteractionResult.PASS;
    }
}
