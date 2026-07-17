package polycube.polycard.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import polycube.polycard.PolyCard;
import polycube.polycard.utils.Helpers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/// World-persistent storage for all players' equipped cards.
public class Storage extends SavedData {

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
    }

    /// Returns the raw UUID-to-player-data map used by the storage codec.
    public Map<UUID, PlayerData> getPlayerDataMap() {
        return Collections.unmodifiableMap(playerDataMap);
    }

    /// Returns this player's data, creating an empty entry on first access.
    public PlayerData getPlayerData(Player player) {
        return playerDataMap.computeIfAbsent(player.getUUID(), _ -> new PlayerData());
    }

    /// Marks this SavedData dirty so Minecraft writes it on the next save.
    public void markDirty() {
        Helpers.debug("Marked card equipment storage dirty");
        this.setDirty();
    }

    /// Loads or creates the world-level PolyCard storage.
    public static Storage load(MinecraftServer server) {
        var storage = server.getDataStorage().computeIfAbsent(TYPE);
        Helpers.debug("Loaded card equipment data for {} player(s)", storage.playerDataMap.size());
        return storage;
    }
}

