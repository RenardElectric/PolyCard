package polycube.polycard.data;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import org.jspecify.annotations.Nullable;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.events.callBacks.CardEventCallback;
import polycube.polycard.utils.Helpers;

import java.util.*;
import java.util.stream.Collectors;

/// Persistent card equipment for one player.
/// Internally cards are stored as a compact type-to-rarity map; public card operations use validated Card values.
public record PlayerData(Map<CardType, RarityLevel> equippedCards) {
    public static final int MAX_EQUIPPED_CARDS = 5;
    public static final Codec<PlayerData> CODEC = Codec.dispatchedMap(CardType.CODEC, _ -> RarityLevel.CODEC).xmap(PlayerData::new, PlayerData::equippedCards);

    public PlayerData() {
        this(new EnumMap<>(CardType.class));
    }

    public PlayerData(Map<CardType, RarityLevel> equippedCards) {
        this.equippedCards = new EnumMap<>(CardType.class);
        for (var cardType : CardType.values()) {
            var rarityLevel = equippedCards.get(cardType);
            if (rarityLevel != null && Card.tryCreate(cardType, rarityLevel).isPresent()) {
                if (this.equippedCards.size() == MAX_EQUIPPED_CARDS) {
                    break;
                }
                this.equippedCards.put(cardType, rarityLevel);
            }
        }
        int ignoredEntries = equippedCards.size() - this.equippedCards.size();
        if (ignoredEntries > 0) {
            Helpers.debug("Ignored {} invalid or excess equipped-card entr{} while loading player data",
                    ignoredEntries, ignoredEntries == 1 ? "y" : "ies");
        }
    }

    /// Exposes equipped cards as a read-only map for threshold checks.
    @Override
    public Map<CardType, RarityLevel> equippedCards() {
        return Collections.unmodifiableMap(equippedCards);
    }

    /// Returns the equipped rarity without allocating a read-only map wrapper on hot event paths.
    public @Nullable RarityLevel equippedRarity(CardType cardType) {
        return equippedCards.get(cardType);
    }

    public int equippedCardCount() {
        return equippedCards.size();
    }

    /// Returns equipped cards as concrete, validated Card instances.
    public Collection<Card> getEquippedCards() {
        var cards = new ArrayList<Card>(equippedCards.size());
        for (var entry : equippedCards.entrySet()) {
            cards.add(new Card(entry.getKey(), entry.getValue()));
        }
        return cards;
    }

    /// Returns whether this exact card is equipped.
    public boolean hasCard(Card card) {
        return hasCard(card.cardType(), card.rarityLevel());
    }

    /// Returns whether this exact type/rarity pair is equipped.
    public boolean hasCard(CardType cardType, RarityLevel rarityLevel) {
        return equippedCards.get(cardType) == rarityLevel;
    }

    /// Returns whether the equipped card satisfies this card's rarity threshold.
    public boolean hasCardOrRarer(Card card) {
        return hasCardOrRarer(card.cardType(), card.rarityLevel());
    }

    /// Returns whether the equipped card for this type is at least the requested rarity.
    public boolean hasCardOrRarer(CardType cardType, RarityLevel rarityLevel) {
        var storedRarityLevel = equippedCards.get(cardType);
        return storedRarityLevel != null && storedRarityLevel.isAtLeast(rarityLevel);
    }

    /// Returns whether any rarity of this card type is equipped.
    public boolean hasCardType(CardType cardType) {
        return equippedCards.containsKey(cardType);
    }

    /// Equips a card if there is room and no card of the same type is already equipped.
    public boolean equipCard(Card card) {
        if (equippedCards.size() >= MAX_EQUIPPED_CARDS) {
            return false;
        }
        if (hasCardType(card.cardType())) {
            return false;
        }
        equippedCards.put(card.cardType(), card.rarityLevel());
        return true;
    }

    /// Unequips this exact card if it is currently equipped.
    public boolean unequipCard(Card card) {
        if (hasCard(card)) {
            equippedCards.remove(card.cardType());
            return true;
        }
        return false;
    }

    /// Unequips whichever rarity is currently equipped for this card type.
    public boolean unequipCardType(CardType cardType) {
        return equippedCards.remove(cardType) != null;
    }

    /// Removes every equipped card without firing card callbacks.
    public void clearEquippedCards() {
        equippedCards.clear();
    }

    /// Creates a GUI-backed container for a player editing their own cards.
    public Container asContainer(ServerPlayer player) {
        return asContainer(player, player);
    }

    /// Creates a GUI-backed container and syncs slot changes back into this PlayerData.
    /// targetPlayer owns the data; feedbackPlayer receives sounds/messages for the edit.
    public Container asContainer(ServerPlayer targetPlayer, ServerPlayer feedbackPlayer) {
        var container = new SimpleContainer(MAX_EQUIPPED_CARDS) {
            @Override
            public void setChanged() {
                var cardsInContainer = items.stream()
                        .map(Card::getCard)
                        .flatMap(Optional::stream)
                        .collect(Collectors.toSet());

                var equippedCardsSet = new HashSet<>(PlayerData.this.getEquippedCards());

                if (equippedCardsSet.equals(cardsInContainer)) {
                    return;
                }

                for (var card : equippedCardsSet) {
                    if (!cardsInContainer.contains(card)) {
                        equippedCards.remove(card.cardType());
                        CardEventCallback.UNEQUIPPED.invoker().onCardUnequip(targetPlayer, card);

                        Helpers.debug("{} unequipped card {} for {}", feedbackPlayer, card, targetPlayer);
                        Helpers.playSound(feedbackPlayer, SoundEvents.BUNDLE_REMOVE_ONE);
                    }
                }

                for (var card : cardsInContainer) {
                    if (!equippedCardsSet.contains(card)) {
                        equippedCards.put(card.cardType(), card.rarityLevel());
                        CardEventCallback.EQUIPPED.invoker().onCardEquip(targetPlayer, card);

                        Helpers.debug("{} equipped card {} for {}", feedbackPlayer, card, targetPlayer);
                        Helpers.playSound(feedbackPlayer, SoundEvents.BUNDLE_INSERT);
                    }
                }

                PolyCard.storage().markDirty();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        };

        int index = 0;
        for (var card : getEquippedCards()) {
            container.items.set(index++, card.asItem());
        }

        return container;
    }
}
