package polycube.polycard.cardEffects.hostile;

import com.google.common.util.concurrent.AtomicDouble;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.KillEventCallback;

public class WitherEffects {
    public static final CardType CARD_TYPE = CardType.WITHER;

    public static final double WITHER_ROSE_DROP_CHANCE = 0.1;

    public static void register() {
        KillEventCallback.EVENT.register(WitherEffects::onKill);
        EntityHurtEventCallback.EVENT.register(WitherEffects::onHurt);
    }

    private static InteractionResult onKill(ServerPlayer player, Entity entity, DamageSource damageSource) {
        if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.RARE)) {
            if (player.getRandom().nextDouble() < WITHER_ROSE_DROP_CHANCE) {
                entity.spawnAtLocation(player.level(), Items.WITHER_ROSE);
            }
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult onHurt(LivingEntity entity, ServerLevel level, DamageSource source, AtomicDouble damage) {
        if (entity instanceof ServerPlayer player) {
            if (source.is(DamageTypes.WITHER) && PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.EPIC)) {
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.PASS;
    }
}
