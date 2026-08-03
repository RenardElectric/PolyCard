package polycube.polycard.cardEffects.misc;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gamerules.GameRules;
import polycube.polycard.PolyCard;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.data.PlayerData;

public class InventoryEffects extends CardEffects implements ServerLivingEntityEvents.AllowDeath, ServerLivingEntityEvents.AfterDeath, ServerLivingEntityEvents.AfterDamage  {

    boolean resetKeepInventory = false;

    @Override
    public boolean allowDeath(LivingEntity entity, DamageSource damageSource, float damageAmount) {
        if (entity instanceof ServerPlayer player && hasCardOrRarer(player, RarityLevel.LEGENDARY)) {
            var gameRules = player.level().getGameRules();
            boolean isKeepInventory = gameRules.get(GameRules.KEEP_INVENTORY);
            if (!isKeepInventory) {
                resetKeepInventory = true;
                gameRules.set(GameRules.KEEP_INVENTORY, true, player.level().getServer());
            }
        }
        return true;
    }

    @Override
    public void afterDeath(LivingEntity entity, DamageSource damageSource) {
        if (entity instanceof ServerPlayer player && resetKeepInventory) {
            var playerData = PolyCard.storage().getPlayerData(player);
            var cardIndex = player.getRandom().nextInt(playerData.equippedCardCount());
            var card = playerData.getEquippedCards().get(cardIndex);
            if (PlayerData.unequipCard(player, card))
                card.previous().ifPresent(previousCard -> PlayerData.equipCard(player, previousCard));
        }
    }

    @Override
    public void afterDamage(LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (entity instanceof ServerPlayer player && resetKeepInventory) {
            player.level().getGameRules().set(GameRules.KEEP_INVENTORY, false, player.level().getServer());
            resetKeepInventory = false;
        }
    }
}
