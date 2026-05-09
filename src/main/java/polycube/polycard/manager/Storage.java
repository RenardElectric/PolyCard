package polycube.polycard.manager;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;

import java.util.*;

public class Storage extends SavedData {
    public static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<Storage> CODEC = Codec.unboundedMap(UUID_CODEC, PlayerData.CODEC)
            .xmap(Storage::new, Storage::getPlayerDataMap);

    private static final SavedDataType<Storage> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, "storage"),
            () -> new Storage(new HashMap<>()),
            CODEC,
            null
    );

    public static final int MAX_EQUIPPED_CARDS = 5;
    public Map<UUID, PlayerData> playerDataMap;

    public Storage(Map<UUID, PlayerData> playerDataMap) {
        this.playerDataMap = new HashMap<>(playerDataMap);
    }

    public Map<UUID, PlayerData> getPlayerDataMap() {
        return playerDataMap;
    }

    public static Storage getSavedStorage(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean hasCard(Player player, Card card) {
        return data(player).hasCard(card);
    }

    public boolean hasCardOrRarer(Player player, Card card) {
        return data(player).hasCardOrRarer(card);
    }

    public List<Card> getEquippedCards(Player player) {
        return data(player).getEquippedCards();
    }

    public boolean equipCard(Player player, Card card) {
        return data(player).equipCard(card);
    }

    public boolean hasCardType(Player player, CardType card) {
        return data(player).hasCardType(card);
    }

    public boolean unequipCard(Player player, Card card) {
        return data(player).unequipCard(card);
    }

    public boolean unequipCardType(Player player, CardType cardType) {
        return data(player).unequipCardType(cardType);
    }

    public void clearEquippedCards(Player player) {
        data(player).clearEquippedCards();
    }

    public PlayerData data(Player player) {
        return playerDataMap.computeIfAbsent(player.getUUID(), _ -> new PlayerData(new ArrayList<>(MAX_EQUIPPED_CARDS)));
    }

    public record PlayerData(List<Card> equippedCards) {
        public static final Codec<PlayerData> CODEC = Card.CODEC.listOf().xmap(PlayerData::new, PlayerData::getEquippedCards);

        public PlayerData {
            equippedCards = new ArrayList<>(equippedCards);
        }

        public boolean hasCard(Card card) {
            return equippedCards.contains(card);
        }

        public boolean hasCardOrRarer(Card card) {
            return equippedCards.stream().anyMatch(equippedCard -> equippedCard.type() == card.type() && equippedCard.rarity().ordinal() >= card.rarity().ordinal());
        }

        public boolean hasCardType(CardType cardType) {
            return equippedCards.stream().anyMatch(equippedCard -> equippedCard.type() == cardType);
        }

        public List<Card> getEquippedCards() {
            return equippedCards;
        }

        public boolean equipCard(Card card) {
            if (equippedCards.size() >= MAX_EQUIPPED_CARDS) {
                return false;
            }
            if (hasCardType(card.type())) {
                return false;
            }
            equippedCards.add(card);
            return true;
        }

        public boolean unequipCard(Card card) {
            return equippedCards.remove(card);
        }

        public boolean unequipCardType(CardType cardType) {
            return equippedCards.removeIf(equippedCard -> equippedCard.type() == cardType);
        }

        public void clearEquippedCards() {
            equippedCards.clear();
        }

        public Container asContainer() {
            var container = new SimpleContainer(MAX_EQUIPPED_CARDS) {
                @Override
                public void setChanged() {
                    clearEquippedCards();
                    for (int i = 0; i < getContainerSize(); i++) {
                        ItemStack stack = getItem(i);
                        CardManager.getCard(stack)
                                .ifPresent(value -> equipCard(value));
                    }
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            };

            int index = 0;
            for (Card equippedCard : equippedCards) {
                container.items.set(index++, equippedCard.asItem());
            }

            return container;
        }
    }
}

