package polycube.polycard.cardEffects.misc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.apache.commons.lang3.mutable.MutableFloat;
import polycube.polycard.card.Card;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.CardEventCallback;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;

public final class SpectatorEffects extends CardEffects implements EntityHurtEventCallback, CardEventCallback.CardUnequipEvent {
    public SpectatorEffects() {
        addAttribute(RarityLevel.LEGENDARY, Attributes.MAX_HEALTH, "spectator_health", AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, -1);
    }

    @Override
    public InteractionResult onEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, MutableFloat damage) {
        if (entity instanceof ServerPlayer player && hasCardOrRarer(player, RarityLevel.LEGENDARY))
            return InteractionResult.FAIL;
        if (source.getEntity() instanceof ServerPlayer player && hasCardOrRarer(player, RarityLevel.LEGENDARY))
            return InteractionResult.FAIL;
        return InteractionResult.PASS;
    }

    @Override
    public void onCardUnequip(ServerPlayer player, Card card) {
        if (card.cardType() == cardType()) player.kill(player.level());
    }
}
