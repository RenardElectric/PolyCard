package polycube.polycard.cardEffects.neutral.ironGolem;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import polycube.polycard.PolyCard;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.manager.CardManager;

import java.util.concurrent.ThreadLocalRandom;

public class IronGolemEffects {
    public static final CardType CARD_TYPE = CardType.IRON_GOLEM;

    public static final int RESISTANCE_ON_ATTACKED_CHANCE = 20;
    public static final int RESISTANCE_DURATION = 200;
    public static final int RESISTANCE_AMPLIFIER = 0;

    public static void registerIronGolemCardEffects(CardManager cardManager) {
        EntityHurtEventCallback.EVENT.register((attacker, level, source) -> onPlayerHit(cardManager, attacker, level, source));
    }

    public static InteractionResult onPlayerHit(CardManager cardManager, LivingEntity entity, ServerLevel level, DamageSource source) {
        if (entity instanceof ServerPlayer player) {
            if (cardManager.getStorage().data(player).hasCardOrRarer(CARD_TYPE, RarityLevel.RARE)) {
                PolyCard.debug("{} has a rare or higher enderman card, removing ender pearl teleport damage", player.getName().getString());
                int random = ThreadLocalRandom.current().nextInt(0, 100);
                if (random < RESISTANCE_ON_ATTACKED_CHANCE) {
                    player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, RESISTANCE_DURATION, RESISTANCE_AMPLIFIER, true, true));
                }
            }
        }
        return InteractionResult.PASS;
    }
}
