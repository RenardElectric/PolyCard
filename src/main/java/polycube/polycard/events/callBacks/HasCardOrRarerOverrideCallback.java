package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;


/// Callback for overriding the result of hasCardOrRarer for a player.
public interface HasCardOrRarerOverrideCallback {
    Event<HasCardOrRarerOverrideCallback> EVENT = EventFactory.createArrayBacked(
            HasCardOrRarerOverrideCallback.class,
            listeners -> (player, cardType, rarityLevel, original) -> {
                for (var listener : listeners) {
                    original = listener.hasCardOrRarerOverride(player, cardType, rarityLevel, original);
                }
                return original;
            }
    );

    boolean hasCardOrRarerOverride(ServerPlayer player, CardType cardType, RarityLevel rarityLevel, boolean original);
}

