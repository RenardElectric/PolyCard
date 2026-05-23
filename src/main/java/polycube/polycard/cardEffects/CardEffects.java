package polycube.polycard.cardEffects;

import polycube.polycard.cardEffects.hostile.ZombieEffects;
import polycube.polycard.cardEffects.neutral.EnderManEffects;
import polycube.polycard.cardEffects.neutral.IronGolemEffects;
import polycube.polycard.cardEffects.neutral.PiglinEffects;
import polycube.polycard.cardEffects.passive.BeeEffects;
import polycube.polycard.cardEffects.passive.CowEffects;
import polycube.polycard.cardEffects.passive.SquidEffects;
import polycube.polycard.manager.CardManager;

/// Registers all card effects for the PolyCard mod.
public class CardEffects {
    public static void registerCardEffects(CardManager cardManager) {
        // Passive
        CowEffects.register(cardManager);
        SquidEffects.register(cardManager);
        BeeEffects.register(cardManager);

        // Neutral
        EnderManEffects.register(cardManager);
        IronGolemEffects.register(cardManager);
        PiglinEffects.register(cardManager);

        // Hostile
        ZombieEffects.register(cardManager);
    }
}
