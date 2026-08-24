package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;

import java.util.Optional;


/// Callback for overriding the rarity level of a card equipped by a player.
public interface EquippedRarityLevelOverrideCallback {
    Event<EquippedRarityLevelOverrideCallback> EVENT = EventFactory.createArrayBacked(
            EquippedRarityLevelOverrideCallback.class,
            listeners -> (player, cardType, rarityLevel) -> {
                for (var listener : listeners) {
                    rarityLevel = listener.equippedRarityLevelOverride(player, cardType, rarityLevel);
                }
                return rarityLevel;
            }
    );

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    Optional<RarityLevel> equippedRarityLevelOverride(ServerPlayer player, CardType cardType, Optional<RarityLevel> originalRarityLevel);
}
