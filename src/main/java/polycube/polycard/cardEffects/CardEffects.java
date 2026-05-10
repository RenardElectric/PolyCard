package polycube.polycard.cardEffects;

import polycube.polycard.cardEffects.neutral.enderEffects.EnderManHandler;
import polycube.polycard.cardEffects.neutral.ironGolem.IronGolemEffects;
import polycube.polycard.cardEffects.passive.cow.CowEffects;
import polycube.polycard.manager.CardManager;

/// Registers all card effects for the PolyCard mod.
public class CardEffects {
    public static void registerCardEffects(CardManager cardManager) {
        // Passive
        CowEffects.registerCowCardEffects(cardManager);

        // Neutral
        EnderManHandler.registerEnderManCardEffects(cardManager);
        IronGolemEffects.registerIronGolemCardEffects(cardManager);
    }
}
