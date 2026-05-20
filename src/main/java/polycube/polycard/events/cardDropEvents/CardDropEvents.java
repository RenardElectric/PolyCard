package polycube.polycard.events.cardDropEvents;

public class CardDropEvents {
    public static void registerCardDropEvents() {
        BreedEvents.register();
        KillEvents.register();
        SummonEvents.register();
    }
}
