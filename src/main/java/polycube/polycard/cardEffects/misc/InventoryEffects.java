package polycube.polycard.cardEffects.misc;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gamerules.GameRules;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.data.PlayerData;

public class InventoryEffects extends CardEffects implements ServerLivingEntityEvents.AllowDeath, ServerLivingEntityEvents.AfterDeath, ServerLivingEntityEvents.AfterDamage  {

    private final PlayerState<Boolean> pendingProtectedDeaths = new PlayerState<>();
    private boolean managesKeepInventory;

    @Override
    public boolean allowDeath(LivingEntity entity, DamageSource damageSource, float damageAmount) {
        if (entity instanceof ServerPlayer player && hasCardOrRarer(player, RarityLevel.LEGENDARY)) {
            var gameRules = player.level().getGameRules();
            pendingProtectedDeaths.put(player, true);
            if (!gameRules.get(GameRules.KEEP_INVENTORY)) {
                managesKeepInventory = true;
                gameRules.set(GameRules.KEEP_INVENTORY, true, player.level().getServer());
            }
        }
        return true;
    }

    @Override
    public void afterDeath(LivingEntity entity, DamageSource damageSource) {
        if (entity instanceof ServerPlayer player && pendingProtectedDeaths.contains(player)) {
            var playerData = playerData(player);
            var cardIndex = player.getRandom().nextInt(playerData.equippedCardCount());
            var card = playerData.getEquippedCards().get(cardIndex);
            PlayerData.downgradeCard(player, card);
        }
    }

    @Override
    public void afterDamage(LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (entity instanceof ServerPlayer player) {
            releaseKeepInventory(player);
        }
    }

    @Override
    protected void onPlayerStateClearing(ServerPlayer player) {
        releaseKeepInventory(player);
    }

    @Override
    protected void onRuntimeClearing() {
        managesKeepInventory = false;
    }

    private void releaseKeepInventory(ServerPlayer player) {
        if (pendingProtectedDeaths.remove(player) == null) {
            return;
        }
        if (managesKeepInventory && pendingProtectedDeaths.isEmpty()) {
            player.level().getGameRules().set(GameRules.KEEP_INVENTORY, false, player.level().getServer());
            managesKeepInventory = false;
        }
    }
}
