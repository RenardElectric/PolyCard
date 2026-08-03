package polycube.polycard.cardEffects.misc;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.jspecify.annotations.Nullable;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.CardEventCallback;
import polycube.polycard.events.callBacks.EquippedRarityLevelOverrideCallback;
import polycube.polycard.events.callBacks.HasCardOrRarerOverrideCallback;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static polycube.polycard.utils.Helpers.decimalFormat;

public class LuckyEffects
        extends CardEffects
        implements ServerTickEvents.EndTick, ServerPlayerEvents.Leave,
        PlayerTickEventCallback, EquippedRarityLevelOverrideCallback, HasCardOrRarerOverrideCallback
{

    public static final int TICK_INTERVAL = 20 * 60 * 5;

    private long tickCounter = 0;
    private final Map<UUID, Card> randomCard = new HashMap<>();

    @Override
    public void onEndTick(MinecraftServer server) {
        tickCounter++;
    }

    @Override
    public void onLeave(ServerPlayer player) {
        var card = randomCard.remove(player.getUUID());
        if (card != null)
            CardEventCallback.UNEQUIPPED.invoker().onCardUnequip(player, card);
    }

    @Override
    public void onPlayerTick(MinecraftServer server, ServerPlayer player) {
        var cardRarityLevel = equippedRarityLevel(player);
        if (cardRarityLevel != null) {
            if (tickCounter % TICK_INTERVAL == 0) {
                var previousCard = randomCard.get(player.getUUID());
                if (previousCard != null) {
                    CardEventCallback.UNEQUIPPED.invoker().onCardUnequip(player, previousCard);
                }

                CardType cardType;
                RarityLevel rarityLevel;
                do {
                    var cardTypeValues = CardType.values();
                    cardType = cardTypeValues[player.getRandom().nextInt(cardTypeValues.length)];
                    var rarityValues = cardType.getRarities();
                    rarityLevel = rarityValues.get(player.getRandom().nextInt(rarityValues.size())).rarityLevel();
                } while (rarityLevel.rank() > cardRarityLevel.rank());

                var card = new Card(cardType, rarityLevel);
                randomCard.put(player.getUUID(), card);
                player.sendSystemMessage(
                        Component.literal("You have been granted a random card for " + decimalFormat(TICK_INTERVAL/1200.0) + " minutes: ")
                                .withStyle(ChatFormatting.GREEN)
                                .append(card.getFormattedName())
                );
                CardEventCallback.EQUIPPED.invoker().onCardEquip(player, card);
            }
        } else {
            var card = randomCard.remove(player.getUUID());
            if (card != null)
                CardEventCallback.UNEQUIPPED.invoker().onCardUnequip(player, card);
        }
    }

    @Override
    public @Nullable RarityLevel equippedRarityLevelOverride(ServerPlayer player, CardType cardType, @Nullable RarityLevel original) {
        var card = randomCard.get(player.getUUID());
        if (card != null && card.cardType() == cardType) {
            if (original == null || card.rarityLevel().rank() > original.rank()) {
                return card.rarityLevel();
            }
        }
        return original;
    }

    @Override
    public boolean hasCardOrRarerOverride(ServerPlayer player, CardType cardType, RarityLevel rarityLevel, boolean original) {
        if (!original) {
            var card = randomCard.get(player.getUUID());
            return card != null && card.cardType() == cardType && card.rarityLevel().rank() >= rarityLevel.rank();
        }
        return true;
    }
}
