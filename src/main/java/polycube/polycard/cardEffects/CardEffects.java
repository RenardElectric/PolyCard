package polycube.polycard.cardEffects;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jspecify.annotations.Nullable;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.data.PlayerData;
import polycube.polycard.data.Storage;
import polycube.polycard.events.EventHandler;
import polycube.polycard.events.callBacks.CardEventCallback;
import polycube.polycard.events.callBacks.EquippedRarityLevelOverrideCallback;
import polycube.polycard.events.callBacks.PlayerLoadEventCallback;
import polycube.polycard.utils.Cooldowns;
import polycube.polycard.utils.TaskScheduler;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class CardEffects extends EventHandler {
    private static final Set<CardEffects> INSTANCES = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<PlayerState<?>> PLAYER_STATES = Collections.newSetFromMap(new IdentityHashMap<>());

    private @Nullable CardType cardType = null;
    private final Map<RarityLevel, Multimap<Holder<Attribute>, Function<ServerPlayer, AttributeModifier>>> attributeMap;

    protected CardEffects() {
        attributeMap = new HashMap<>();
    }

    /// Assigns the owning card before making this effect visible to any event invoker.
    public final void initialize(CardType cardType) {
        if (this.cardType != null) throw new IllegalStateException(getClass().getName() + " is already initialized");
        this.cardType = cardType;
        registerCallbacks();
        if (!attributeMap.isEmpty()) registerAttributesCallbacks();
        INSTANCES.add(this);
    }

    /// Clears every effect's transient state for a player who is leaving.
    public static void clearPlayerState(ServerPlayer player) {
        for (var effect : INSTANCES) {
            effect.onPlayerStateClearing(player);
        }
        PLAYER_STATES.forEach(state -> state.remove(player));
    }

    /// Clears all transient effect states when a server shuts down.
    public static void clearRuntimeState() {
        for (var effect : INSTANCES) {
            effect.onRuntimeClearing();
        }
        PLAYER_STATES.forEach(PlayerState::clear);
    }

    /// Hook for state that needs player-aware cleanup before its slots are discarded.
    protected void onPlayerStateClearing(ServerPlayer player) {}

    /// Hook for non-player transient state that must reset between server instances.
    protected void onRuntimeClearing() {}

    public final CardType cardType() {
        return Objects.requireNonNull(cardType, "Cannot access CardType before initialize() is called");
    }

    /// Returns the storage for the server currently invoking this effect.
    protected final Storage storage() {
        return PolyCard.runtime().storage();
    }

    /// Returns the player data for the server currently invoking this effect.
    protected final PlayerData playerData(ServerPlayer player) {
        return PolyCard.runtime().storage().getPlayerData(player);
    }

    /// Returns the scheduler for the server currently invoking this effect.
    protected final TaskScheduler scheduler() {
        return PolyCard.runtime().scheduler();
    }

    /// Returns the cooldowns for the server currently invoking this effect.
    protected final Cooldowns cooldowns() {
        return PolyCard.runtime().cooldowns();
    }

    public final boolean hasCardOrRarer(ServerPlayer player, RarityLevel rarityLevel) {
        return equippedRarityLevel(player)
                .filter(equippedRarity -> equippedRarity.isAtLeast(rarityLevel))
                .isPresent();
    }

    public final Optional<RarityLevel> equippedRarityLevel(ServerPlayer player) {
        var original = playerData(player).equippedRarityLevel(cardType());
        return EquippedRarityLevelOverrideCallback.EVENT.invoker().equippedRarityLevelOverride(player, cardType(), original);
    }

    public final Optional<Card> equippedCard(ServerPlayer player) {
        var rarityLevel = equippedRarityLevel(player);
        return rarityLevel.flatMap(rarity -> Card.tryCreate(cardType(), rarity));
    }

    /// Returns only the card owned by persistent equipment, excluding temporary overrides.
    protected final Optional<Card> persistentCard(ServerPlayer player) {
        return playerData(player).equippedRarityLevel(cardType())
                .flatMap(rarity -> Card.tryCreate(cardType(), rarity));
    }

    /// Returns whether another nearby player has this card type at the requested rarity or higher.
    public final boolean nearPlayerWithCard(ServerPlayer player, RarityLevel minRarityLevel, double distanceSquared) {
        var level = player.level();
        var playerPos = player.position();
        return !level.getPlayers(p ->
                !p.equals(player)
                        && p.position().distanceToSqr(playerPos) <= distanceSquared
                        && hasCardOrRarer(p, minRarityLevel), 1).isEmpty();
    }

    protected final void registerAttributesCallbacks() {
        PlayerLoadEventCallback.EVENT.register(this::loadAttributes);
        CardEventCallback.EQUIPPED.register((player, card) -> {
            if (card.cardType() == cardType) this.loadAttributes(player);
        });
        CardEventCallback.UNEQUIPPED.register((player, card) -> {
            if (card.cardType() == cardType) this.removeAttributes(player, card.rarityLevel());
        });
    }

    @SuppressWarnings("NullableProblems")
    protected final void addAttribute(RarityLevel rarityLevel, Holder<Attribute> attribute, String id, AttributeModifier.Operation operation, double amount) {
        attributeMap.computeIfAbsent(rarityLevel, _ -> HashMultimap.create())
                .put(attribute, _ -> new AttributeModifier(Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, id), amount, operation));
    }

    @SuppressWarnings("NullableProblems")
    protected final void addAttribute(RarityLevel rarityLevel, Holder<Attribute> attribute, String id, AttributeModifier.Operation operation, Function<ServerPlayer, Double> amountFunction) {
        attributeMap.computeIfAbsent(rarityLevel, _ -> HashMultimap.create())
                .put(attribute, player -> new AttributeModifier(Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, id), amountFunction.apply(player), operation));
    }

    protected final void loadAttributes(ServerPlayer player) {
        equippedRarityLevel(player).ifPresent(
                rarityLevel -> setCardAttributes(player, rarityLevel, player.getAttributes()::addTransientAttributeModifiers)
        );
    }

    protected final void removeAttributes(ServerPlayer player, RarityLevel rarityLevel) {
        setCardAttributes(player, rarityLevel, player.getAttributes()::removeAttributeModifiers);
    }

    private void setCardAttributes(ServerPlayer player, RarityLevel rarityLevel, Consumer<Multimap<Holder<Attribute>, AttributeModifier>> consumer) {
        int maxRank = rarityLevel.rank();
        for (int i = 0; i <= maxRank; i++) {
            Optional.ofNullable(attributeMap.get(RarityLevel.BY_RANK.get(i)))
                    .map(map -> Multimaps.transformValues(map, f -> f.apply(player)))
                    .ifPresent(consumer);
        }
    }

    /// Captures the player's current rarity thresholds for this card type.
    public final CardRarityConditions conditionsFor(ServerPlayer player) {
        return new CardRarityConditions(player);
    }

    @SuppressWarnings({"unused", "UnusedReturnValue"})
    public final class CardRarityConditions {
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private final Optional<RarityLevel> equippedRarity;

        private CardRarityConditions(ServerPlayer player) {
            equippedRarity = equippedRarityLevel(player);
        }

        /// Runs when the player has this card type at common or higher.
        public CardRarityConditions hasCommon(Runnable runnable) {
            if (hasAtLeast(RarityLevel.COMMON)) runnable.run();
            return this;
        }

        /// Runs when the player has this card type at uncommon or higher.
        public CardRarityConditions hasUncommon(Runnable runnable) {
            if (hasAtLeast(RarityLevel.UNCOMMON)) runnable.run();
            return this;
        }

        /// Runs when the player has this card type at rare or higher.
        public CardRarityConditions hasRare(Runnable runnable) {
            if (hasAtLeast(RarityLevel.RARE)) runnable.run();
            return this;
        }

        /// Runs when the player has this card type at epic or higher.
        public CardRarityConditions hasEpic(Runnable runnable) {
            if (hasAtLeast(RarityLevel.EPIC)) runnable.run();
            return this;
        }

        /// Runs when the player has this card type at legendary.
        public CardRarityConditions hasLegendary(Runnable runnable) {
            if (hasAtLeast(RarityLevel.LEGENDARY)) runnable.run();
            return this;
        }

        /// Runs one branch depending on whether common-or-higher is unlocked.
        public CardRarityConditions hasCommon(Runnable runnable, Runnable elseCaseRunnable) {
            if (hasAtLeast(RarityLevel.COMMON)) runnable.run();
            else elseCaseRunnable.run();
            return this;
        }

        /// Runs one branch depending on whether uncommon-or-higher is unlocked.
        public CardRarityConditions hasUncommon(Runnable runnable, Runnable elseCaseRunnable) {
            if (hasAtLeast(RarityLevel.UNCOMMON)) runnable.run();
            else elseCaseRunnable.run();
            return this;
        }

        /// Runs one branch depending on whether rare-or-higher is unlocked.
        public CardRarityConditions hasRare(Runnable runnable, Runnable elseCaseRunnable) {
            if (hasAtLeast(RarityLevel.RARE)) runnable.run();
            else elseCaseRunnable.run();
            return this;
        }

        /// Runs one branch depending on whether epic-or-higher is unlocked.
        public CardRarityConditions hasEpic(Runnable runnable, Runnable elseCaseRunnable) {
            if (hasAtLeast(RarityLevel.EPIC)) runnable.run();
            else elseCaseRunnable.run();
            return this;
        }

        /// Runs one branch depending on whether legendary is unlocked.
        public CardRarityConditions hasLegendary(Runnable runnable, Runnable elseCaseRunnable) {
            if (hasAtLeast(RarityLevel.LEGENDARY)) runnable.run();
            else elseCaseRunnable.run();
            return this;
        }

        private boolean hasAtLeast(RarityLevel rarity) {
            return equippedRarity.map(rarityLevel -> rarityLevel.isAtLeast(rarity)).orElse(false);
        }
    }

    /// A typed player-keyed slot whose lifecycle is managed by CardEffects.
    protected static final class PlayerState<T> {
        private final Map<UUID, T> values = new HashMap<>();

        public PlayerState() {
            PLAYER_STATES.add(this);
        }

        public @Nullable T get(ServerPlayer player) {
            return values.get(player.getUUID());
        }

        public T getOrDefault(ServerPlayer player, T defaultValue) {
            return values.getOrDefault(player.getUUID(), defaultValue);
        }

        public @Nullable T put(ServerPlayer player, T value) {
            return values.put(player.getUUID(), value);
        }

        public @Nullable T remove(ServerPlayer player) {
            return values.remove(player.getUUID());
        }

        public boolean contains(ServerPlayer player) {
            return values.containsKey(player.getUUID());
        }

        public boolean isEmpty() {
            return values.isEmpty();
        }

        private void clear() {
            values.clear();
        }
    }
}
