package polycube.polycard.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.inventory.ContainerInput;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.gui.EquipmentGUI;

import java.util.List;

public final class CommandAndGuiGameTests {
    @GameTest
    public void publicCommandsAndValidInfoAreRegistered(GameTestHelper helper) throws CommandSyntaxException {
        var player = GameTestSupport.player(helper);
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        var source = player.createCommandSourceStack();

        helper.assertTrue(dispatcher.execute("polycard", source) == 1,
                "The base PolyCard command must be registered");
        helper.assertTrue(dispatcher.execute("polycard help", source) == 1,
                "The help command must be available to players");
        helper.assertTrue(dispatcher.execute("polycard cooldown", source) == 1,
                "The cooldown command must be available to players");
        helper.assertTrue(dispatcher.execute("polycard info passive chicken", source) == 1,
                "A card type inside the selected group must be accepted");
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void mismatchedCardGroupIsRejected(GameTestHelper helper) throws CommandSyntaxException {
        var player = GameTestSupport.player(helper);
        var source = player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS);
        int result = helper.getLevel().getServer().getCommands().getDispatcher()
                .execute("polycard info passive creeper", source);

        helper.assertTrue(result == 0,
                "A card type outside the selected group must be rejected");
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void administrativeGiveSupportsDefaultAndExplicitRarities(GameTestHelper helper) throws CommandSyntaxException {
        var player = GameTestSupport.player(helper);
        var source = player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS);
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();

        helper.assertTrue(dispatcher.execute("polycard give @s passive chicken", source) == 1,
                "Give without a rarity must use the card type's minimum rarity");
        helper.assertTrue(dispatcher.execute("polycard give @s neutral bee epic", source) == 1,
                "Give with a supported explicit rarity must succeed");
        helper.assertTrue(countCard(player, new Card(CardType.CHICKEN, RarityLevel.COMMON)) == 1,
                "Default give must deliver a common Chicken card");
        helper.assertTrue(countCard(player, new Card(CardType.BEE, RarityLevel.EPIC)) == 1,
                "Explicit give must deliver the requested card");
        helper.assertTrue(dispatcher.execute("polycard give @s misc nether common", source) == 0,
                "Give must reject a rarity unsupported by that card type");
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void combineUsesSlotFreedByExactlyTenCards(GameTestHelper helper) throws CommandSyntaxException {
        var player = GameTestSupport.player(helper);
        var inventory = player.getInventory();
        GameTestSupport.fillNonEquipmentInventory(player);
        inventory.setSelectedSlot(0);
        var sourceCards = new Card(CardType.CHICKEN, RarityLevel.COMMON).asItem();
        sourceCards.setCount(Card.CARDS_FOR_NEXT_LEVEL);
        inventory.setSelectedItem(sourceCards);

        int result = executeCombine(helper, player);
        var combinedCard = Card.getCard(inventory.getSelectedItem());
        helper.assertTrue(result == 1,
                "Exactly ten cards must combine when their source slot becomes free");
        helper.assertTrue(combinedCard.filter(card -> card.rarityLevel() == RarityLevel.UNCOMMON).isPresent(),
                "The upgraded card must be placed in the newly freed slot");
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void combineRejectsLossyFullInventory(GameTestHelper helper) throws CommandSyntaxException {
        var player = GameTestSupport.player(helper);
        var inventory = player.getInventory();
        GameTestSupport.fillNonEquipmentInventory(player);
        inventory.setSelectedSlot(0);
        var sourceCards = new Card(CardType.CHICKEN, RarityLevel.COMMON).asItem();
        sourceCards.setCount(Card.CARDS_FOR_NEXT_LEVEL + 1);
        inventory.setSelectedItem(sourceCards);

        helper.assertTrue(executeCombine(helper, player) == 0,
                "Combine must fail instead of dropping an upgraded card from a full inventory");
        helper.assertTrue(inventory.getSelectedItem().getCount() == Card.CARDS_FOR_NEXT_LEVEL + 1,
                "A rejected combine must not consume source cards");
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void combineUsesExistingCompatibleStackSpace(GameTestHelper helper) throws CommandSyntaxException {
        var player = GameTestSupport.player(helper);
        var inventory = player.getInventory();
        GameTestSupport.fillNonEquipmentInventory(player);
        inventory.setSelectedSlot(0);
        var sourceCards = new Card(CardType.CHICKEN, RarityLevel.COMMON).asItem();
        sourceCards.setCount(Card.CARDS_FOR_NEXT_LEVEL + 1);
        inventory.setSelectedItem(sourceCards);
        var upgradedCards = new Card(CardType.CHICKEN, RarityLevel.UNCOMMON).asItem();
        upgradedCards.setCount(63);
        inventory.setItem(1, upgradedCards);

        helper.assertTrue(executeCombine(helper, player) == 1,
                "Combine must use a compatible stack even when no slot is empty");
        helper.assertTrue(inventory.getSelectedItem().getCount() == 1,
                "A successful combine must consume exactly ten source cards");
        helper.assertTrue(inventory.getItem(1).getCount() == 64,
                "The upgraded card must join its compatible stack");
        player.discard();
        helper.succeed();
    }

    @GameTest
    public void concurrentEquipmentEditorsMergeCommittedChanges(GameTestHelper helper) {
        var target = GameTestSupport.player(helper);
        var viewer1 = GameTestSupport.player(helper);
        var viewer2 = GameTestSupport.player(helper);
        var original = new Card(CardType.CHICKEN, RarityLevel.COMMON);
        var firstEdit = new Card(CardType.PIGLIN, RarityLevel.UNCOMMON);
        var secondEdit = new Card(CardType.BEE, RarityLevel.RARE);

        GameTestSupport.equip(helper, target, List.of(original));
        EquipmentGUI.openEquipmentGUI(viewer1, target);
        EquipmentGUI.openEquipmentGUI(viewer2, target);
        viewer1.containerMenu.setCarried(firstEdit.asItem());
        viewer1.containerMenu.clicked(0, 0, ContainerInput.PICKUP, viewer1);
        viewer2.containerMenu.setCarried(secondEdit.asItem());
        viewer2.containerMenu.clicked(1, 0, ContainerInput.PICKUP, viewer2);

        var equippedCards = GameTestSupport.data(target).equippedCards();
        helper.assertTrue(equippedCards.get(firstEdit.cardType()) == firstEdit.rarityLevel(),
                "A second editor must not overwrite the first editor's committed card");
        helper.assertTrue(equippedCards.get(secondEdit.cardType()) == secondEdit.rarityLevel(),
                "The second editor's card must also be committed");
        helper.assertTrue(!equippedCards.containsKey(original.cardType()),
                "The card replaced by the first editor must stay replaced");

        viewer1.closeContainer();
        viewer2.closeContainer();
        target.discard();
        viewer1.discard();
        viewer2.discard();
        helper.succeed();
    }

    @GameTest
    public void equipmentEditorClosesWhenEitherParticipantRespawns(GameTestHelper helper) {
        var target = GameTestSupport.player(helper);
        var viewer = GameTestSupport.player(helper);
        var replacement = GameTestSupport.player(helper);
        EquipmentGUI.openEquipmentGUI(viewer, target);
        var staleMenu = viewer.containerMenu;
        ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(target, replacement, false);

        helper.assertTrue(viewer.containerMenu != staleMenu,
                "An equipment editor must close when its target player instance is replaced");
        EquipmentGUI.openEquipmentGUI(viewer, target);
        var viewerMenu = viewer.containerMenu;
        ServerPlayerEvents.LEAVE.invoker().onLeave(viewer);
        helper.assertTrue(viewer.containerMenu != viewerMenu,
                "An equipment editor must close when its viewer leaves");
        target.discard();
        viewer.discard();
        replacement.discard();
        helper.succeed();
    }

    private static int executeCombine(GameTestHelper helper, net.minecraft.server.level.ServerPlayer player)
            throws CommandSyntaxException {
        return helper.getLevel().getServer().getCommands().getDispatcher()
                .execute("polycard combine", player.createCommandSourceStack());
    }

    private static int countCard(net.minecraft.server.level.ServerPlayer player, Card expected) {
        int count = 0;
        for (var stack : player.getInventory()) {
            if (Card.getCard(stack).filter(expected::equals).isPresent()) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
