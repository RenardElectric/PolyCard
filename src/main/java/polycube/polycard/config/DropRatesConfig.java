package polycube.polycard.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;
import polycube.polycard.PolyCard;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityDistribution;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/// Loads complete valid snapshots. Invalid files retain the last committed settings in memory,
/// or code defaults before any valid config has loaded. Existing files are never rewritten.
public final class DropRatesConfig {
    public static final String FILE_NAME = "polycard.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static volatile DropRateSettings active = DropRateSettings.defaults();
    private static volatile @Nullable Prepared pending;
    private static boolean reloading;

    private DropRatesConfig() {}

    public static RarityDistribution distribution(CardType type) {
        return active.distribution(type);
    }

    /// Loot builders use the candidate; gameplay keeps the committed rates until reload succeeds.
    public static RarityDistribution lootDistribution(CardType type) {
        var candidate = pending;
        return (candidate == null ? active : candidate.settings()).distribution(type);
    }

    public static synchronized void load(Path path) {
        if (reloading) throw new IllegalStateException("Cannot replace drop rates during a data-pack reload");
        prepare(path).ifPresent(DropRatesConfig::commit);
    }

    public static synchronized void beginReload(Path path) {
        if (reloading) throw new IllegalStateException("A drop-rate reload is already in progress");
        pending = prepare(path).orElse(null);
        reloading = true;
    }

    public static synchronized void finishReload(boolean success) {
        var candidate = pending;
        if (success && candidate != null) {
            commit(candidate);
        } else if (!success && candidate != null) {
            PolyCard.LOGGER.warn("Data-pack reload failed; retaining the previous drop rates");
        }
        pending = null;
        reloading = false;
    }

    private static void commit(Prepared prepared) {
        active = prepared.settings();
        PolyCard.LOGGER.info("Loaded drop rates from {}", prepared.path());
    }

    private static Optional<Prepared> prepare(Path path) {
        try {
            var result = read(path);
            result.error().ifPresent(error -> warnRetained(path, error.message()));
            // Do not accept codec partial results: every configured entry must be valid.
            return result.result().map(settings -> new Prepared(path, settings));
        } catch (IOException | JsonParseException exception) {
            warnRetained(path, exception.toString());
            return Optional.empty();
        }
    }

    private static DataResult<DropRateSettings> read(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return DropRateSettings.CODEC.parse(JsonOps.INSTANCE, GsonHelper.parse(reader));
        } catch (NoSuchFileException exception) {
            var defaults = DropRateSettings.defaults();
            writeDefaults(path, defaults);
            return DataResult.success(defaults);
        }
    }

    private static void warnRetained(Path path, String reason) {
        PolyCard.LOGGER.warn("Could not load {}: {}; retaining the last valid drop rates (or defaults)", path, reason);
    }

    /// Creates the initial example through the same codec, without replacing an existing config.
    private static void writeDefaults(Path path, DropRateSettings defaults) throws IOException {
        var encoded = DropRateSettings.CODEC.encodeStart(JsonOps.INSTANCE, defaults).getOrThrow(IllegalStateException::new);
        var target = path.toAbsolutePath();
        Files.createDirectories(target.getParent());
        var temporary = Files.createTempFile(target.getParent(), target.getFileName() + ".", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(encoded) + System.lineSeparator(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private record Prepared(Path path, DropRateSettings settings) {}
}
