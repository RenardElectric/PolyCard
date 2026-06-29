package polycube.polycard.cardEffects.neutral;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.KillEventCallback;
import polycube.polycard.utils.CardRarityConditions;
import polycube.polycard.utils.Helpers;

import java.util.*;

public class WolfEffects {
    public static final CardType CARD_TYPE = CardType.WOLF;

    public static final int RESISTANCE_AMPLIFIER = 0;
    public static final int STRENGTH_AMPLIFIER = 0;
    public static final int LOW_HEALTH_THRESHOLD = 8;
    public static final int IMPROVED_STRENGTH_AMPLIFIER = 1;
    public static final int IMPROVED_SPEED_AMPLIFIER = 1;
    public static final float HEAL_PERCENTAGE = 0.25f;

    private static final Map<UUID, Set<Wolf>> playerKill = new HashMap<>();

    public static void register() {
        Helpers.runTaskTimer(0, 1, server -> {
            for (var level : server.getAllLevels()) {
                for (var wolf : level.getEntities(EntityTypes.WOLF, LivingEntity::isAlive)) {
                    if (wolf.getRootOwner() instanceof ServerPlayer player) {
                        CardRarityConditions.of(player, CARD_TYPE)
                            .hasUncommon(() -> wolf.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 2, RESISTANCE_AMPLIFIER, true, false)))
                            .hasRare(() -> wolf.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 2, STRENGTH_AMPLIFIER, true, false)))
                            .hasLegendary(() -> {
                                if (player.getHealth() < LOW_HEALTH_THRESHOLD) {
                                    wolf.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 2, IMPROVED_STRENGTH_AMPLIFIER, true, false));
                                    wolf.addEffect(new MobEffectInstance(MobEffects.SPEED, 2, IMPROVED_SPEED_AMPLIFIER, true, false));
                                }
                            });
                        var wolfset = playerKill.get(player.getUUID());
                        if (wolfset != null) {
                            if (!wolfset.contains(wolf)) {
                                wolfset.add(wolf);
                                wolf.setHealth(wolf.getHealth() + wolf.getMaxHealth() * HEAL_PERCENTAGE);
                                Helpers.debug("{} has the epic wolf card, healing {} by {} half hearts", player.getName().getString(), wolf.getName().getString(), HEAL_PERCENTAGE * wolf.getMaxHealth());
                            } else {
                                playerKill.remove(player.getUUID());
                            }
                        }
                    }
                }
            }
        });

        KillEventCallback.EVENT.register(WolfEffects::onKill);
    }

    private static void onKill(ServerPlayer player, Entity entity, DamageSource source) {
        if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.EPIC)) {
            playerKill.putIfAbsent(player.getUUID(), new HashSet<>());
        }
    }
}
