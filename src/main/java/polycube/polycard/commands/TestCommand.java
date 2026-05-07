package polycube.polycard.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Rarity;
import polycube.polycard.manager.CardManager;

public class TestCommand extends PolyCardCommand {
    private final CardManager cardManager;

    public TestCommand(CardManager cardManager) {
        super(
                "test",
                "Test the card rolling system (admin only).",
                "/" + PolyCard.MOD_ID + " test",
                PermissionLevel.ALL
        );
        this.cardManager = cardManager;
    }

    @Override
    protected int execute(CommandSourceStack source) {

        int common = 0;
        int uncommon = 0;
        int rare = 0;
        int epic = 0;
        int legendary = 0;
        for (int i = 0; i < 1000; i++) {
            switch (cardManager.getRandomRarity(Rarity.COMMON)) {
                case COMMON -> common++;
                case UNCOMMON -> uncommon++;
                case RARE -> rare++;
                case EPIC -> epic++;
                case LEGENDARY -> legendary++;
            }
        }
        var message = Component.literal("Roll: " + common + " / " + uncommon + " / " + rare +" / " + epic + " / " + legendary);
        source.sendSuccess(() -> message, false);
        source.sendSuccess(() -> Component.literal("Expected: 500 / 250 / 150 / 90 / 10"), false);

        return 1;
    }
}
