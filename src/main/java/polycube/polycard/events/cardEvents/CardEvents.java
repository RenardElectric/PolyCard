package polycube.polycard.events.cardEvents;

import polycube.polycard.events.cardEvents.neutral.enderMan.EnderManHandler;
import polycube.polycard.events.cardEvents.neutral.ironGolem.IronGolemHandler;
import polycube.polycard.events.cardEvents.passive.cow.CowHandler;
import polycube.polycard.manager.CardManager;

public class CardEvents {
    public static void registerCardEvents(CardManager cardManager) {
        // Passive
        CowHandler.registerCowCardEvents(cardManager);

        // Neutral
        EnderManHandler.registerEnderManCardEvents(cardManager);
        IronGolemHandler.registerIronGolemCardEvents(cardManager);
    }
}
