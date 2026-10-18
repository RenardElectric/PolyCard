package polycube.polycard.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import polycube.polycard.PolyCard;
import polycube.polycard.card.CardType;
import polycube.polycard.card.Rarity;
import polycube.polycard.card.RarityDistribution;
import polycube.polycard.card.RarityLevel;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/// Per-card drop rate overrides read from `config/polycard.json`.
///
/// The probabilities in CardType are the defaults. The file only changes the roll chance of
/// rarities a card already supports; tiers cannot be added or removed. A card whose entry is
/// invalid keeps all its defaults, and a malformed file keeps every default, so a bad edit never
/// stops the server. Missing cards and rarities are written back with their defaults.
public final class DropRatesConfig {
    public static final String FILE_NAME = "polycard.json";
    public static final int VERSION = 1;
    private static final String VERSION_KEY = "version";
    private static final String DROP_RATES_KEY = "drop_rates";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static volatile Map<CardType, RarityDistribution> distributions = Map.of();

    private DropRatesConfig() {}

    /// Returns the configured distribution for a card type, or its code default.
    public static RarityDistribution distribution(CardType cardType) {
        var distribution = distributions.get(cardType);
        return distribution != null ? distribution : cardType.definition().rarityDistribution();
    }

    /// Reads the config file, creating it with every default when it does not exist yet.
    public static void load(Path path) {
        JsonObject root;
        boolean fileExists = Files.exists(path);
        if (fileExists) {
            try {
                var element = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
                if (!element.isJsonObject()) {
                    throw new JsonParseException("the top level must be a JSON object");
                }
                root = element.getAsJsonObject();
            } catch (IOException | JsonParseException e) {
                PolyCard.LOGGER.error("Could not read {}, using default drop rates: {}", path, e.getMessage());
                distributions = Map.of();
                return;
            }
        } else {
            root = new JsonObject();
        }

        var result = parse(root);
        result.warnings().forEach(warning -> PolyCard.LOGGER.warn("{}: {}", path.getFileName(), warning));
        distributions = result.distributions();

        if (!fileExists || result.addedDefaults()) {
            try {
                write(path, root);
            } catch (IOException e) {
                PolyCard.LOGGER.error("Could not write {}: {}", path, e.getMessage());
            }
        }
        PolyCard.LOGGER.info("Loaded drop rates from {} ({} warning(s))", path, result.warnings().size());
    }

    /// Validates a config tree and fills in any missing card or rarity with its default.
    public static ParseResult parse(JsonObject root) {
        var warnings = new ArrayList<String>();
        boolean addedDefaults = false;

        if (!root.has(VERSION_KEY)) {
            root.addProperty(VERSION_KEY, VERSION);
            addedDefaults = true;
        } else if (!isNumber(root.get(VERSION_KEY)) || root.get(VERSION_KEY).getAsInt() > VERSION) {
            warnings.add("unsupported version " + root.get(VERSION_KEY) + ", reading it as version " + VERSION);
        }

        var dropRatesElement = root.get(DROP_RATES_KEY);
        if (dropRatesElement != null && !dropRatesElement.isJsonObject()) {
            warnings.add("'" + DROP_RATES_KEY + "' must be an object, using default drop rates");
            return new ParseResult(Map.of(), List.copyOf(warnings), addedDefaults);
        }
        if (dropRatesElement == null) {
            dropRatesElement = new JsonObject();
            root.add(DROP_RATES_KEY, dropRatesElement);
            addedDefaults = true;
        }
        var dropRates = dropRatesElement.getAsJsonObject();

        for (var cardId : dropRates.keySet()) {
            if (CardType.deserialize(cardId).isEmpty()) {
                warnings.add("unknown card '" + cardId + "' is ignored");
            }
        }

        var parsed = new EnumMap<CardType, RarityDistribution>(CardType.class);
        for (var cardType : CardType.values()) {
            var cardId = cardType.getSerializedName();
            var defaults = cardType.definition().rarityDistribution().rarities();

            var cardElement = dropRates.get(cardId);
            if (cardElement == null) {
                dropRates.add(cardId, defaultsObject(defaults));
                addedDefaults = true;
                continue;
            }
            if (!cardElement.isJsonObject()) {
                warnings.add("'" + cardId + "' must be an object, using its default drop rates");
                continue;
            }
            var cardRates = cardElement.getAsJsonObject();

            for (var rarityId : cardRates.keySet()) {
                var rarityLevel = RarityLevel.BY_ID.get(rarityId);
                if (rarityLevel == null || !cardType.hasRarity(rarityLevel)) {
                    warnings.add("'" + cardId + "' has no '" + rarityId + "' rarity, it is ignored");
                }
            }

            var rarities = new ArrayList<Rarity>(defaults.size());
            boolean valid = true;
            for (var rarity : defaults) {
                var rarityId = rarity.rarityLevel().getSerializedName();
                var value = cardRates.get(rarityId);
                if (value == null) {
                    cardRates.add(rarityId, plainNumber(rarity.probability()));
                    addedDefaults = true;
                    rarities.add(rarity);
                } else if (isNumber(value)) {
                    rarities.add(new Rarity(rarity.rarityLevel(), value.getAsFloat(), rarity.isEnchanted(), rarity.description()));
                } else {
                    warnings.add("'" + cardId + "." + rarityId + "' must be a number, using the default drop rates of '" + cardId + "'");
                    valid = false;
                    break;
                }
            }
            if (!valid) {
                continue;
            }

            try {
                parsed.put(cardType, RarityDistribution.of(List.copyOf(rarities)));
            } catch (IllegalArgumentException e) {
                warnings.add("invalid drop rates for '" + cardId + "' (" + e.getMessage() + "), using its defaults");
            }
        }

        return new ParseResult(Map.copyOf(parsed), List.copyOf(warnings), addedDefaults);
    }

    /// Replaces the active overrides; an empty map restores every default. Intended for tests.
    public static void setDistributions(Map<CardType, RarityDistribution> newDistributions) {
        distributions = Map.copyOf(newDistributions);
    }

    private static JsonObject defaultsObject(List<Rarity> rarities) {
        var object = new JsonObject();
        for (var rarity : rarities) {
            object.add(rarity.rarityLevel().getSerializedName(), plainNumber(rarity.probability()));
        }
        return object;
    }

    /// Writes 0.0008 instead of Gson's default 8.0E-4 so the file stays easy to edit.
    private static JsonPrimitive plainNumber(float value) {
        return new JsonPrimitive(new BigDecimal(Float.toString(value)).stripTrailingZeros());
    }

    private static boolean isNumber(JsonElement element) {
        return element instanceof JsonPrimitive primitive && primitive.isNumber();
    }

    private static void write(Path path, JsonObject root) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        var tempPath = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(tempPath, GSON.toJson(root) + System.lineSeparator(), StandardCharsets.UTF_8);
        Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /// Distributions read from a config tree, with every problem found and whether defaults were filled in.
    public record ParseResult(Map<CardType, RarityDistribution> distributions, List<String> warnings, boolean addedDefaults) {}
}
