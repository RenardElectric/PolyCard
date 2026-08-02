package polycube.polycard.cardEffects;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
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
import polycube.polycard.events.EventHandler;
import polycube.polycard.events.callBacks.CardEventCallback;
import polycube.polycard.events.callBacks.PlayerLoadEventCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class CardEffects extends EventHandler {
    private @Nullable CardType cardType = null;
    private @Nullable Map<RarityLevel, Multimap<Holder<Attribute>, AttributeModifier>> attributeMap = null;

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
        return PolyCard.storage().getPlayerData(player).hasCardOrRarer(cardType(), rarityLevel);
    }

    @SuppressWarnings({"SameParameterValue", "NullableProblems"})
    protected void addAttribute(RarityLevel rarityLevel, Holder<Attribute> attribute, String id, double amount, AttributeModifier.Operation operation) {

        if (attributeMap == null) {
            attributeMap = new HashMap<>();

            PlayerLoadEventCallback.EVENT.register(this::loadPlayerAttributes);
            CardEventCallback.EQUIPPED.register((player, card) -> this.setCardAttributes(card, player.getAttributes()::addTransientAttributeModifiers));
            CardEventCallback.UNEQUIPPED.register((player, card) -> this.setCardAttributes(card, player.getAttributes()::removeAttributeModifiers));
        }

        attributeMap.computeIfAbsent(rarityLevel, _ -> HashMultimap.create())
                .put(attribute, new AttributeModifier(Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, id), amount, operation));
    }

    private void loadPlayerAttributes(ServerPlayer player) {
        var rarityLevel = PolyCard.storage().getPlayerData(player).equippedRarityLevel(cardType());
        if (rarityLevel != null) {
            setCardAttributes(new Card(cardType(), rarityLevel), player.getAttributes()::removeAttributeModifiers);
        }
    }

    private void setCardAttributes(Card card, Consumer<Multimap<Holder<Attribute>, AttributeModifier>> consumer) {
        if (attributeMap == null || card.cardType() != cardType()) return;
        int maxRank = card.rarityLevel().rank();
        for (int i = 0; i <= maxRank; i++) {
            Optional.ofNullable(attributeMap.get(RarityLevel.BY_RANK.get(i)))
                    .ifPresent(consumer);
        }
    }
}
