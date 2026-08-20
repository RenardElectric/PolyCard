package polycube.polycard.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import polycube.polycard.PolyCard;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/// World-persistent storage for all players' equipped cards.
public final class Storage extends SavedData {

    public static final Codec<Storage> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerData.CODEC)
            .xmap(Storage::new, Storage::getPlayerDataMap);

    @SuppressWarnings("DataFlowIssue")
    private static final SavedDataType<Storage> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, "storage"),
            () -> new Storage(new HashMap<>()),
            CODEC,
            null
    );

    private final Map<UUID, PlayerData> playerDataMap;

    public Storage(Map<UUID, PlayerData> playerDataMap) {
        this.playerDataMap = new HashMap<>(playerDataMap);
        var repairedPlayers = new HashMap<UUID, Map<CardType, RarityLevel>>();
        int discardedCardCount = 0;

        for (var entry : playerDataMap.entrySet()) {
            var discardedCards = entry.getValue().discardedCards;
            if (discardedCards.isEmpty()) continue;
            repairedPlayers.put(entry.getKey(), discardedCards);
            discardedCardCount += discardedCards.size();
        }

        if (!repairedPlayers.isEmpty()) {
            setDirty();
            PolyCard.LOGGER.warn("Repaired invalid stored equipment for {} player(s); discarded {} card(s)", repairedPlayers.size(), discardedCardCount);
            PolyCard.LOGGER.debug("Discarded stored equipment entries: {}", repairedPlayers);
        }
    }

    /// Returns the raw UUID-to-player-data map used by the storage codec.
    public Map<UUID, PlayerData> getPlayerDataMap() {
        return Collections.unmodifiableMap(playerDataMap);
    }

    /// Returns this player's data, creating an empty entry on first access.
    public PlayerData getPlayerData(Player player) {
        return playerDataMap.computeIfAbsent(player.getUUID(), _ -> new PlayerData());
    }

    /// Loads or creates the world-level PolyCard storage.
    public static Storage load(MinecraftServer server) {
        var storage = server.getDataStorage().computeIfAbsent(TYPE);
        PolyCard.LOGGER.debug("Loaded card equipment storage (players={})", storage.playerDataMap.size());
        return storage;
    }
}

