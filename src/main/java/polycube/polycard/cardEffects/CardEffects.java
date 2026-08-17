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
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.events.EventHandler;
import polycube.polycard.events.callBacks.CardEventCallback;
import polycube.polycard.events.callBacks.EquippedRarityLevelOverrideCallback;
import polycube.polycard.events.callBacks.HasCardOrRarerOverrideCallback;
import polycube.polycard.events.callBacks.PlayerLoadEventCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class CardEffects extends EventHandler {
    private @Nullable CardType cardType = null;
    private @Nullable Map<RarityLevel, Multimap<Holder<Attribute>, Function<ServerPlayer, AttributeModifier>>> attributeMap = null;

    protected CardEffects() {}

    /// Assigns the owning card before making this effect visible to any event invoker.
    public final void initialize(CardType cardType) {
        if (this.cardType != null) {
            throw new IllegalStateException(getClass().getName() + " is already initialized");
        }
        this.cardType = Objects.requireNonNull(cardType, "cardType");
        registerCallbacks();
    }

    public CardType cardType() {
        return Objects.requireNonNull(cardType, "Cannot access CardType before initialize() is called");
    }

    public boolean hasCardOrRarer(ServerPlayer player, RarityLevel rarityLevel) {
        var original = PolyCard.storage().getPlayerData(player).hasCardOrRarer(cardType(), rarityLevel);
        return HasCardOrRarerOverrideCallback.EVENT.invoker().hasCardOrRarerOverride(player, cardType(), rarityLevel, original);
    }

    public @Nullable RarityLevel equippedRarityLevel(ServerPlayer player) {
        var original = PolyCard.storage().getPlayerData(player).equippedRarityLevel(cardType());
        return EquippedRarityLevelOverrideCallback.EVENT.invoker().equippedRarityLevelOverride(player, cardType(), original);
    }

    /// Returns whether another nearby player has this card type at the requested rarity or higher.
    public boolean nearPlayerWithCard(ServerPlayer player, RarityLevel minRarityLevel, double distanceSquared) {
        var level = player.level();
        var playerPos = player.position();
        return !level.getPlayers(p ->
                !p.equals(player)
                        && p.position().distanceToSqr(playerPos) <= distanceSquared
                        && hasCardOrRarer(p, minRarityLevel), 1).isEmpty();
    }

    protected void registerAttributesCallbacks() {
        PlayerLoadEventCallback.EVENT.register(this::loadAttributes);
        CardEventCallback.EQUIPPED.register((player, card) -> {
            if (card.cardType() == cardType) this.loadAttributes(player);
        });
        CardEventCallback.UNEQUIPPED.register((player, card) -> {
            if (card.cardType() == cardType) this.removeAttributes(player, card.rarityLevel());
        });
    }

    @SuppressWarnings("NullableProblems")
    protected void addAttribute(RarityLevel rarityLevel, Holder<Attribute> attribute, String id, AttributeModifier.Operation operation, double amount) {
        if (attributeMap == null) {
            attributeMap = new HashMap<>();
            registerAttributesCallbacks();
        }
        attributeMap.computeIfAbsent(rarityLevel, _ -> HashMultimap.create())
                .put(attribute, _ -> new AttributeModifier(Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, id), amount, operation));
    }

    @SuppressWarnings("NullableProblems")
    protected void addAttribute(RarityLevel rarityLevel, Holder<Attribute> attribute, String id, AttributeModifier.Operation operation, Function<ServerPlayer, Double> amountFunction) {
        if (attributeMap == null) {
            attributeMap = new HashMap<>();
            registerAttributesCallbacks();
        }
        attributeMap.computeIfAbsent(rarityLevel, _ -> HashMultimap.create())
                .put(attribute, player -> new AttributeModifier(Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, id), amountFunction.apply(player), operation));
    }

    protected void loadAttributes(ServerPlayer player) {
        var rarityLevel = equippedRarityLevel(player);
        if (rarityLevel != null) setCardAttributes(player, rarityLevel, player.getAttributes()::addTransientAttributeModifiers);
    }

    protected void removeAttributes(ServerPlayer player, RarityLevel rarityLevel) {
        setCardAttributes(player, rarityLevel, player.getAttributes()::removeAttributeModifiers);
    }

    private void setCardAttributes(ServerPlayer player, RarityLevel rarityLevel, Consumer<Multimap<Holder<Attribute>, AttributeModifier>> consumer) {
        if (attributeMap == null) return;
        int maxRank = rarityLevel.rank();
        for (int i = 0; i <= maxRank; i++) {
            Optional.ofNullable(attributeMap.get(RarityLevel.BY_RANK.get(i)))
                    .map(map -> Multimaps.transformValues(map, f -> f.apply(player)))
                    .ifPresent(consumer);
        }
    }

    /// Captures the player's current rarity thresholds for this card type.
    public CardRarityConditions conditionsFor(ServerPlayer player) {
        return new CardRarityConditions(player);
    }

    @SuppressWarnings({"unused", "UnusedReturnValue"})
    public final class CardRarityConditions {
        private final @Nullable RarityLevel equippedRarity;

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
            return equippedRarity != null && equippedRarity.isAtLeast(rarity);
        }
    }
}
