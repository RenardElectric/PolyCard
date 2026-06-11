package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import polycube.polycard.card.Card;

public interface CardEventCallback {
    Event<CardEventCallback> EQUIPPED = EventFactory.createArrayBacked(CardEventCallback.class,
            (listeners) -> (player, card) -> {
                for (var listener : listeners) {
                    listener.cardEvent(player, card);
                }
            });

    Event<CardEventCallback> UNEQUIPPED = EventFactory.createArrayBacked(CardEventCallback.class,
            (listeners) -> (player, card) -> {
                for (var listener : listeners) {
                    listener.cardEvent(player, card);
                }
            });

    void cardEvent(ServerPlayer player, Card card);
}
