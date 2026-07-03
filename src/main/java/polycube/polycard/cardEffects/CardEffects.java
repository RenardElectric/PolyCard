package polycube.polycard.cardEffects;

import polycube.polycard.card.CardType;
import polycube.polycard.events.EventHandler;

public abstract class CardEffects extends EventHandler {
    public CardType cardType;
    public void setCardType(CardType cardType) {
        this.cardType = cardType;
    }
}
