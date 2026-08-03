package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;


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

    @Nullable RarityLevel equippedRarityLevelOverride(ServerPlayer player, CardType cardType, @Nullable RarityLevel original);
}
