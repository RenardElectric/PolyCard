package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;

import java.util.Optional;


/// Callback for overriding the rarity level of a card equipped by a player.
public interface CardProbabilityOverrideCallback {
    Event<CardProbabilityOverrideCallback> EVENT = EventFactory.createArrayBacked(
            CardProbabilityOverrideCallback.class,
            listeners -> (player, cardType, rarityLevel, originalRarity) -> {
                for (var listener : listeners) {
                    originalRarity = listener.cardProbabilityOverride(player, cardType, rarityLevel, originalRarity);
                }
                return originalRarity;
            }
    );

    float cardProbabilityOverride(ServerPlayer player, CardType cardType, RarityLevel rarityLevel, float originalRarity);
}
