package polycube.polycard.cardEffects;

import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import polycube.polycard.PolyCard;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.events.EventHandler;

import java.util.Objects;

public abstract class CardEffects extends EventHandler {
    private @Nullable CardType cardType;

    protected CardEffects() {
    }

    /// Assigns the owning card before making this effect visible to any event invoker.
    public final void initialize(CardType cardType) {
        if (this.cardType != null) {
            throw new IllegalStateException(getClass().getName() + " is already initialized");
        }
        this.cardType = Objects.requireNonNull(cardType, "cardType");
        registerCallbacks();
    }

    public CardType cardType() {
        return Objects.requireNonNull(cardType, "Cannot access CardType before initialize() is called");
    }

    public boolean hasCardOrRarer(ServerPlayer player, RarityLevel rarityLevel) {
        return PolyCard.storage().getPlayerData(player).hasCardOrRarer(cardType(), rarityLevel);
    }
}
