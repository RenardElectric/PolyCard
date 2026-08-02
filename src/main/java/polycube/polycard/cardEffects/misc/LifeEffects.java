package polycube.polycard.cardEffects.misc;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.data.PlayerData;

public final class LifeEffects extends CardEffects implements ServerLivingEntityEvents.AfterDeath {

    public LifeEffects() {
        addAttribute(RarityLevel.COMMON, Attributes.MAX_HEALTH, "life_common", 2, AttributeModifier.Operation.ADD_VALUE);
        addAttribute(RarityLevel.UNCOMMON, Attributes.MAX_HEALTH, "life_uncommon", 4, AttributeModifier.Operation.ADD_VALUE);
        addAttribute(RarityLevel.RARE, Attributes.MAX_HEALTH, "life_rare", 4, AttributeModifier.Operation.ADD_VALUE);
        addAttribute(RarityLevel.EPIC, Attributes.MAX_HEALTH, "life_epic", 4, AttributeModifier.Operation.ADD_VALUE);
        addAttribute(RarityLevel.LEGENDARY, Attributes.MAX_HEALTH, "life_legendary", 6, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public void afterDeath(LivingEntity entity, DamageSource damageSource) {
        if (entity instanceof ServerPlayer player) {
            var rarityLevel = PolyCard.storage().getPlayerData(player).equippedRarityLevel(cardType());
            if (rarityLevel != null) {
                PlayerData.unequipCard(player, new Card(cardType(), rarityLevel));
                rarityLevel.previous().ifPresent(
                        previousRarity -> PlayerData.equipCard(player, new Card(cardType(), previousRarity))
                );
            }
        }
    }
}
