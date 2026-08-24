package polycube.polycard.cardEffects.misc;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import polycube.polycard.PolyCard;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.data.PlayerData;

public final class LifeEffects extends CardEffects implements ServerLivingEntityEvents.AfterDeath {

    public LifeEffects() {
        addAttribute(RarityLevel.COMMON, Attributes.MAX_HEALTH, "life_common", AttributeModifier.Operation.ADD_VALUE, 2);
        addAttribute(RarityLevel.UNCOMMON, Attributes.MAX_HEALTH, "life_uncommon", AttributeModifier.Operation.ADD_VALUE, 4);
        addAttribute(RarityLevel.RARE, Attributes.MAX_HEALTH, "life_rare", AttributeModifier.Operation.ADD_VALUE, 6);
        addAttribute(RarityLevel.EPIC, Attributes.MAX_HEALTH, "life_epic", AttributeModifier.Operation.ADD_VALUE, 8);
        addAttribute(RarityLevel.LEGENDARY, Attributes.MAX_HEALTH, "life_legendary", AttributeModifier.Operation.ADD_VALUE, 10);
    }

    @Override
    public void afterDeath(LivingEntity entity, DamageSource damageSource) {
        if (entity instanceof ServerPlayer player) {
            equippedCard(player).ifPresent(card ->
                    PlayerData.downgradeCard(player, card).mapOrElse(
                    _ -> {
                        PolyCard.LOGGER.debug("{} lost one Life-card tier after death ({})", player.getName().getString(), card);
                        return true;
                    },
                    error -> {
                        PolyCard.LOGGER.warn("Failed to downgrade {} for {} after death: {}", card, player.getName().getString(), error.message());
                        return false;
                    }
            ));
        }
    }
}
