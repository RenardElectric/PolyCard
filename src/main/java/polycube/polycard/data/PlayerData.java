package polycube.polycard.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.events.callBacks.CardEventCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/// Persistent card equipment for one player.
///
/// All equipment invariants are owned by [Equipment]; this class adds persistence
/// and gameplay event delivery around committed changes.
public final class PlayerData {
    public static final Codec<PlayerData> CODEC = Codec.dispatchedMap(CardType.CODEC, _ -> RarityLevel.CODEC)
            .xmap(PlayerData::new, PlayerData::equippedCards);

    private Equipment equipment;
    public final Map<CardType, RarityLevel> discardedCards;

    public PlayerData() {
        this(Map.of());
    }

    public PlayerData(Map<CardType, RarityLevel> equippedCards) {
        var equipmentRepair = Equipment.repair(equippedCards);
        this.equipment = equipmentRepair.repaired();
        this.discardedCards = equipmentRepair.discarded();
    }

    /// Exposes equipped cards as a read-only map for persistence and callers.
    public Map<CardType, RarityLevel> equippedCards() {
        return equipment.cardsByType();
    }

    /// Returns the equipped rarity without allocating a card list on hot event paths.
    public @Nullable RarityLevel equippedRarityLevel(CardType cardType) {
        return equipment.rarityLevel(cardType);
    }

    public int equippedCardCount() {
        return equipment.size();
    }

    public List<Card> getEquippedCards() {
        return equipment.cards();
    }

    @SuppressWarnings("unused")
    public boolean hasCard(CardType cardType, RarityLevel rarityLevel) {
        return equipment.contains(cardType, rarityLevel);
    }

    public boolean hasCardOrRarer(CardType cardType, RarityLevel rarityLevel) {
        return equipment.containsAtLeast(cardType, rarityLevel);
    }

    @SuppressWarnings("unused")
    public boolean hasCardType(CardType cardType) {
        return equipment.containsType(cardType);
    }

    /// Validates a complete proposed equipment set without changing persistent state.
    public DataResult<List<Card>> canSetEquippedCards(Collection<Card> cards) {
        return Equipment.create(cards).map(Equipment::cards);
    }

    /// Equips a card, atomically replacing a different rarity or mutex-group peer.
    public static DataResult<EquipmentChange> equipOrReplaceCard(ServerPlayer player, Card card) {
        var playerData = PolyCard.runtime().storage().getPlayerData(player);
        return applyEquipment(player, playerData.equipment.equipOrReplace(card));
    }

    /// Atomically replaces the complete equipment set after validating every invariant.
    public static DataResult<EquipmentChange> setEquippedCards(ServerPlayer player, Collection<Card> cards) {
        return applyEquipment(player, Equipment.create(cards));
    }

    /// Atomically lowers an equipped card by one supported rarity, or removes its minimum tier.
    public static DataResult<EquipmentChange> downgradeCard(ServerPlayer player, Card card) {
        var playerData = PolyCard.runtime().storage().getPlayerData(player);
        return applyEquipment(player, playerData.equipment.downgrade(card));
    }

    private static DataResult<EquipmentChange> applyEquipment(ServerPlayer player, DataResult<Equipment> proposedEquipment) {
        var playerData = PolyCard.runtime().storage().getPlayerData(player);
        return proposedEquipment.map(target -> {
            var before = playerData.equipment.cardsByType();
            var after = target.cardsByType();
            if (before.equals(after)) {
                return EquipmentChange.NONE;
            }

            var unequipped = changedCards(before, after);
            var equipped = changedCards(after, before);
            playerData.equipment = target;

            unequipped.forEach(card -> CardEventCallback.UNEQUIPPED.invoker().onCardUnequip(player, card));
            equipped.forEach(card -> CardEventCallback.EQUIPPED.invoker().onCardEquip(player, card));
            PolyCard.runtime().storage().setDirty();
            return new EquipmentChange(unequipped, equipped);
        });
    }

    private static List<Card> changedCards(
            Map<CardType, RarityLevel> source,
            Map<CardType, RarityLevel> comparison
    ) {
        var changed = new ArrayList<Card>();
        source.forEach((cardType, rarityLevel) -> {
            if (comparison.get(cardType) != rarityLevel) {
                changed.add(new Card(cardType, rarityLevel));
            }
        });
        return List.copyOf(changed);
    }

    /// Cards removed and added by one committed equipment transaction.
    public record EquipmentChange(List<Card> unequipped, List<Card> equipped) {
        private static final EquipmentChange NONE = new EquipmentChange(List.of(), List.of());
    }
}
