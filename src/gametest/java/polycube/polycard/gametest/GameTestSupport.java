package polycube.polycard.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.data.PlayerData;

import java.util.Collection;

final class GameTestSupport {
    private GameTestSupport() {}

    @SuppressWarnings("removal")
    static ServerPlayer player(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        Vec3 position = helper.absoluteVec(new Vec3(1.5D, 2.0D, 1.5D));
        player.setPos(position);
        return player;
    }

    static PlayerData data(ServerPlayer player) {
        return PolyCard.runtime().storage().getPlayerData(player);
    }

    static void equip(GameTestHelper helper, ServerPlayer player, Collection<Card> cards) {
        PlayerData.setEquippedCards(player, cards).ifError(error ->
                helper.fail("Could not arrange test equipment: " + error.message()));
    }

    static void fillNonEquipmentInventory(ServerPlayer player) {
        var inventory = player.getInventory();
        inventory.clearContent();
        for (int slot = 0; slot < inventory.getNonEquipmentItems().size(); slot++) {
            inventory.setItem(slot, new ItemStack(Items.STONE, 64));
        }
    }
}
