package polycube.polycard.events.cardLootEvents;

public class CardLootEvents {
    public static void registerCardDropEvents() {
        BreedEvents.register();
        KillEvents.register();
        SummonEvents.register();
        UseItemOnEvents.register();
    }
}
