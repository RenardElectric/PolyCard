package polycube.polycard.cardEffects.misc;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import polycube.polycard.PolyCard;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.KeepInventoryEventCallback;

public class InventoryEffects extends CardEffects implements ServerPlayerEvents.AfterRespawn, KeepInventoryEventCallback {
    @Override
    public void afterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        if(oldPlayer.getUUID() == newPlayer.getUUID() && hasCardOrRarer(newPlayer, RarityLevel.LEGENDARY)) {
            var playerData = playerData(newPlayer);
            if (playerData.equippedCardCount() <= 1) return;
            var cardIndex = newPlayer.getRandom().nextInt(playerData.equippedCardCount());
            var card = playerData.getEquippedCards().get(cardIndex);
            PlayerData.downgradeCard(newPlayer, card).mapOrElse(
                    _ -> {
                        PolyCard.LOGGER.debug(
                                "{} consumed Legendary Inventory-card death protection and downgraded {}",
                                newPlayer.getName().getString(), card
                        );
                        return true;
                    },
                    error -> {
                        PolyCard.LOGGER.warn(
                                "Failed to downgrade {} for {} after Inventory-card death protection: {}",
                                card, newPlayer.getName().getString(), error.message()
                        );
                        return false;
                    }
            );
        }
    }

    @Override
    public InteractionResult onKeepInventory(Player player, Level world, boolean original) {
        if (player instanceof ServerPlayer serverPlayer && hasCardOrRarer(serverPlayer, RarityLevel.LEGENDARY)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
