package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import polycube.polycard.card.Card;

public final class CardEventCallback {
    public static final Event<CardEquipEvent> EQUIPPED = EventFactory.createArrayBacked(CardEquipEvent.class,
            (listeners) -> (player, card) -> {
                for (var listener : listeners) {
                    listener.onCardEquip(player, card);
                }
            });

    public static final Event<CardUnequipEvent> UNEQUIPPED = EventFactory.createArrayBacked(CardUnequipEvent.class,
            (listeners) -> (player, card) -> {
                for (var listener : listeners) {
                    listener.onCardUnequip(player, card);
                }
            });

    public interface CardEquipEvent {
        void onCardEquip(ServerPlayer player, Card card);
    }

    public interface CardUnequipEvent {
        void onCardUnequip(ServerPlayer player, Card card);
    }
}
