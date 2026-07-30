package polycube.polycard.cardEffects.misc;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.data.PlayerData;

public class LifeEffects extends CardEffects implements ServerLivingEntityEvents.AfterDeath {
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
