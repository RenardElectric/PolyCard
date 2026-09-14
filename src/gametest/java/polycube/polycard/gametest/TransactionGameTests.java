package polycube.polycard.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.events.CardItemUseEvent;

import java.util.List;

public final class TransactionGameTests {
    @GameTest
    public void cardUseIgnoresNonCardsAndSneakingPlayers(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        var handler = new CardItemUseEvent();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        helper.assertTrue(handler.onItemUse(player, helper.getLevel(), InteractionHand.MAIN_HAND) == InteractionResult.PASS,
                "Using a non-card must preserve normal item behavior");

        var card = new Card(CardType.CHICKEN, RarityLevel.COMMON);
        player.setItemInHand(InteractionHand.MAIN_HAND, card.asItem());
        player.setShiftKeyDown(true);
        helper.assertTrue(handler.onItemUse(player, helper.getLevel(), InteractionHand.MAIN_HAND) == InteractionResult.PASS,
                "Sneaking must leave card-item use untouched");
        helper.assertTrue(GameTestSupport.data(player).equippedCardCount() == 0,
                "Ignored card use must not mutate equipment");
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void cardUseEquipsAndConsumesOneCard(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        var card = new Card(CardType.CHICKEN, RarityLevel.COMMON);
        var stack = card.asItem();
        stack.setCount(2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        var result = new CardItemUseEvent().onItemUse(player, helper.getLevel(), InteractionHand.MAIN_HAND);
        helper.assertTrue(result == InteractionResult.SUCCESS,
                "Using a valid unequipped card must succeed");
        helper.assertTrue(GameTestSupport.data(player).hasCard(card.cardType(), card.rarityLevel()),
                "Successful card use must equip the card");
        helper.assertTrue(player.getMainHandItem().getCount() == 1,
                "Successful card use must consume exactly one item");
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void cardSwapRejectsLossyFullInventory(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        var inventory = player.getInventory();
        var equipped = new Card(CardType.CHICKEN, RarityLevel.COMMON);
        var replacement = new Card(CardType.CHICKEN, RarityLevel.UNCOMMON);
        GameTestSupport.equip(helper, player, List.of(equipped));

        GameTestSupport.fillNonEquipmentInventory(player);
        inventory.setSelectedSlot(0);
        var source = replacement.asItem();
        source.setCount(2);
        inventory.setSelectedItem(source);

        var result = new CardItemUseEvent().onItemUse(player, helper.getLevel(), InteractionHand.MAIN_HAND);
        helper.assertTrue(result == InteractionResult.FAIL,
                "A swap must fail when it cannot return the replaced card safely");
        helper.assertTrue(GameTestSupport.data(player).hasCard(equipped.cardType(), equipped.rarityLevel()),
                "A rejected swap must preserve the equipped card");
        helper.assertTrue(inventory.getSelectedItem().getCount() == 2,
                "A rejected swap must not consume the held card");
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void singleOffhandCardSwapsInPlace(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        var equipped = new Card(CardType.CHICKEN, RarityLevel.COMMON);
        var replacement = new Card(CardType.CHICKEN, RarityLevel.UNCOMMON);
        GameTestSupport.equip(helper, player, List.of(equipped));

        GameTestSupport.fillNonEquipmentInventory(player);
        player.setItemInHand(InteractionHand.OFF_HAND, replacement.asItem());
        var result = new CardItemUseEvent().onItemUse(player, helper.getLevel(), InteractionHand.OFF_HAND);
        var returnedCard = Card.getCard(player.getItemInHand(InteractionHand.OFF_HAND));

        helper.assertTrue(result == InteractionResult.SUCCESS,
                "A one-card off-hand swap must succeed even when main inventory is full");
        helper.assertTrue(returnedCard.filter(equipped::equals).isPresent(),
                "The replaced card must be returned directly to the initiating hand");
        helper.assertTrue(GameTestSupport.data(player).hasCard(replacement.cardType(), replacement.rarityLevel()),
                "The replacement card must be committed");
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void cardSwapUsesCompatibleStackSpace(GameTestHelper helper) {
        var player = GameTestSupport.player(helper);
        var inventory = player.getInventory();
        var equipped = new Card(CardType.CHICKEN, RarityLevel.COMMON);
        var replacement = new Card(CardType.CHICKEN, RarityLevel.UNCOMMON);
        GameTestSupport.equip(helper, player, List.of(equipped));

        GameTestSupport.fillNonEquipmentInventory(player);
        inventory.setSelectedSlot(0);
        var source = replacement.asItem();
        source.setCount(2);
        inventory.setSelectedItem(source);
        var compatible = equipped.asItem();
        compatible.setCount(63);
        inventory.setItem(1, compatible);

        var result = new CardItemUseEvent().onItemUse(player, helper.getLevel(), InteractionHand.MAIN_HAND);
        helper.assertTrue(result == InteractionResult.SUCCESS,
                "A swap must use compatible stack space when no slot is empty");
        helper.assertTrue(inventory.getSelectedItem().getCount() == 1,
                "A successful swap must consume one held card");
        helper.assertTrue(inventory.getItem(1).getCount() == 64,
                "The replaced card must join its compatible stack");
        player.discard();
        helper.succeed();
    }
}
