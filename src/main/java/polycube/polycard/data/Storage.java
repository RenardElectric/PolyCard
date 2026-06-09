package polycube.polycard.data;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import polycube.polycard.PolyCard;
import polycube.polycard.utils.Helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/// Manages the storage of player card data for the PolyCard mod,
/// allowing players to equip and unequip cards
/// and saving this data persistently on the server.
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

    private final Map<UUID, PlayerData> playerDataMap;

    public Storage(Map<UUID, PlayerData> playerDataMap) {
        this.playerDataMap = new HashMap<>(playerDataMap);
    }

    /// Returns the map of player UUIDs to their corresponding PlayerData.
    ///
    /// @return the map of player UUIDs to PlayerData
    public Map<UUID, PlayerData> getPlayerDataMap() {
        return playerDataMap;
    }

    /// Retrieves the PlayerData for a given player,
    /// creating a new entry if it doesn't exist.
    ///
    /// @param player the player to retrieve the data for
    /// @return the PlayerData associated with the player
    public PlayerData getPlayerData(Player player) {
        return playerDataMap.computeIfAbsent(player.getUUID(), _ -> new PlayerData());
    }

    /// Mark the storage as dirty to save it on the next server tick.
    public void save() {
        Helpers.debug("Marking storage as dirty for saving.");
        this.setDirty();
    }

    /// Load the storage for this card manager from the server's saved data.
    ///
    /// @param server The server to load the storage from.
    public static Storage load(MinecraftServer server) {
        Helpers.debug("Storage loaded.");
        return server.getDataStorage().computeIfAbsent(TYPE);
    }
}

