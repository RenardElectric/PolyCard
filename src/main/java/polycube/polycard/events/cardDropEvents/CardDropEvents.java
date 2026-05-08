package polycube.polycard.events.cardDropEvents;

public class CardDropEvents {
    public static void registerCardDropEvents() {
        BreedEvents.registerBreedEvents();
        KillEvents.registerKillEvents();
    }
}
