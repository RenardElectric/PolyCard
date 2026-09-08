package polycube.polycard.cardEffects.misc;

import net.minecraft.server.level.ServerPlayer;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.CardProbabilityOverrideCallback;

public class LootEffects  extends CardEffects implements CardProbabilityOverrideCallback {
    public static final float PROBABILITY_MULTIPLIER = 1.2f;

    @Override
    public float cardProbabilityOverride(ServerPlayer player, CardType cardType, RarityLevel rarityLevel, float originalRarity) {
        return (float) (originalRarity * Math.pow(PROBABILITY_MULTIPLIER, rarityLevel.rank()));
    }
}
