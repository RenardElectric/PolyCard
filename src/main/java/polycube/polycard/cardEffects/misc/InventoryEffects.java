package polycube.polycard.cardEffects.misc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.KeepInventoryEventCallback;

public class InventoryEffects extends CardEffects implements KeepInventoryEventCallback {
    @Override
    public InteractionResult onKeepInventory(Player player, Level world, boolean original) {
        if (player instanceof ServerPlayer serverPlayer && hasCardOrRarer(serverPlayer, RarityLevel.LEGENDARY)) {
            var playerData = playerData(serverPlayer);
            return playerData.equippedCardCount() == 1 ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }
}
