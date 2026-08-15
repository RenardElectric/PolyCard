package polycube.polycard.card;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import polycube.polycard.PolyCard;

import java.util.*;
import java.util.function.Function;

/// A concrete card that can exist in-game.
/// The constructor enforces that the card type supports the selected rarity;
/// use tryCreate when reading untrusted data from commands, storage, or item stacks.
public record Card(CardType cardType, RarityLevel rarityLevel) {

    public static final int CARDS_FOR_NEXT_LEVEL = 10;
    public static final Item CARD_ITEM = Items.POISONOUS_POTATO;

    private static final String CARD_TYPE_KEY = "cardType";
    private static final String RARITY_LEVEL_KEY = "rarityLevel";
    private static final Map<Card, ItemStackTemplate> itemStackCache = new HashMap<>();

    public Card {
        Objects.requireNonNull(cardType, "cardType");
        Objects.requireNonNull(rarityLevel, "rarityLevel");
        if (!cardType.hasRarity(rarityLevel)) {
            throw new IllegalArgumentException(cardType + " does not support " + rarityLevel + " rarity.");
        }
    }

    /// Creates a card from a raw type/rarity pair if that pair is valid.
    public static Optional<Card> tryCreate(CardType cardType, RarityLevel rarityLevel) {
        if (!cardType.hasRarity(rarityLevel)) {
            return Optional.empty();
        }
        return Optional.of(new Card(cardType, rarityLevel));
    }

    /// Returns this card's configured rarity data.
    public Rarity rarity() {
        return cardType.getRarity(rarityLevel).orElseThrow();
    }

    /// Returns the next higher card if this card type supports that rarity.
    public Optional<Card> next() {
        return rarityLevel.next()
                .filter(cardType::hasRarity)
                .map(nextRarityLevel -> new Card(cardType, nextRarityLevel));
    }

    /// Returns the previous lower card if this card type supports that rarity.
    public Optional<Card> previous() {
        return rarityLevel.previous()
                .filter(cardType::hasRarity)
                .map(prevRarityLevel -> new Card(cardType, prevRarityLevel));
    }

    /// Creates an ItemStack carrying this card's identifying data and display components.
    public ItemStack asItem() {
        return getItemTemplate().create();
    }

    /// Templates are immutable for a card, so cache them instead of rebuilding hover/item components.
    public ItemStackTemplate getItemTemplate() {
        return itemStackCache.computeIfAbsent(this, Card::createCardItemTemplate);
    }

    /// Returns a colored, hoverable card name for chat messages.
    public Component getFormattedName() {
        return ComponentUtils.wrapInSquareBrackets(
                Component.literal(toString()).withStyle(
                        s -> s.withHoverEvent(
                                new HoverEvent.ShowItem(getItemTemplate())
                        )
                )
        ).withStyle(rarityLevel.color());
    }

    /// Returns the configured roll probability for this exact card.
    @SuppressWarnings("unused")
    public float getProbability() {
        return rarity().probability();
    }

    /// Returns whether this card should render with the enchantment glint.
    public boolean isEnchanted() {
        return rarity().isEnchanted();
    }

    /// Builds lore for the acquisition condition and all effects unlocked up to this rarity.
    public List<Component> getDescriptions() {
        var descriptions = new ArrayList<Component>();
        descriptions.add(
                Component.literal("(Acquired by " + cardType.getCondition() + ")")
                        .withStyle(s -> s.withColor(ChatFormatting.DARK_GRAY).withItalic(false))
        );
        for (var rarity : cardType.getRarities()) {
            descriptions.add(rarity.getFormattedDescription());
            if (rarity.rarityLevel() == rarityLevel) break;
        }
        return descriptions;
    }

    /// Returns the item model identifier path, such as "polycard:cardGroup/card_type/rarity_level".
    public Identifier getId() {
        return Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, cardType.getFullId() + "/" + rarityLevel.getSerializedName());
    }

    @Override
    public String toString() {
        return rarityLevel + " " + cardType + " Card";
    }

    /// Creates the base stack template for a card item.
    private static ItemStackTemplate createCardItemTemplate(Card card) {
        var rarityLevel = card.rarityLevel();
        var cardType = card.cardType();

        var cardTypeName = cardType.getSerializedName();
        var rarityLevelName = rarityLevel.getSerializedName();

        var customDataTag = new CompoundTag();
        var polyCardTag = new CompoundTag();
        polyCardTag.putString(CARD_TYPE_KEY, cardTypeName);
        polyCardTag.putString(RARITY_LEVEL_KEY, rarityLevelName);
        customDataTag.put(PolyCard.MOD_ID, polyCardTag);

        var components = DataComponentPatch.builder()
                .set(DataComponents.ITEM_NAME, Component.literal(card.toString()).withStyle(rarityLevel.color()))
                .set(DataComponents.LORE, new ItemLore(card.getDescriptions()))
                .set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, card.isEnchanted())
                .set(DataComponents.CUSTOM_DATA, CustomData.of(customDataTag))
                .set(DataComponents.MAX_STACK_SIZE, 64)
                .set(DataComponents.ITEM_MODEL, card.getId())
                .remove(DataComponents.CONSUMABLE)
                .remove(DataComponents.FOOD);

        return new ItemStackTemplate(CARD_ITEM, components.build());
    }

    /// Returns whether the stack contains a valid PolyCard payload.
    @SuppressWarnings("unused")
    public static boolean isCard(ItemStack item) {
        return getCard(item).isPresent();
    }

    /// Reads a concrete Card from an ItemStack's custom data.
    public static Optional<Card> getCard(ItemStack item) {
        var cardType = getCardType(item);
        var rarity = getCardRarity(item);
        return cardType.flatMap(type -> rarity.flatMap(rarityLevel -> tryCreate(type, rarityLevel)));
    }

    /// Reads the stored card type without requiring the full card pair to be valid.
    public static Optional<CardType> getCardType(ItemStack item) {
        return getCardData(item, CARD_TYPE_KEY, CardType::deserialize);
    }

    /// Reads the stored rarity level without requiring the full card pair to be valid.
    public static Optional<RarityLevel> getCardRarity(ItemStack item) {
        return getCardData(item, RARITY_LEVEL_KEY, RarityLevel::deserialize);
    }

    /// Extracts one value from the PolyCard custom data compound.
    private static <T> Optional<T> getCardData(ItemStack item, String key, Function<String, Optional<T>> fromId) {
        var customData = item.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return Optional.empty();
        return customData.copyTag()
                .getCompound(PolyCard.MOD_ID)
                .flatMap(compoundTag -> compoundTag.getString(key)
                        .flatMap(fromId));
    }
}
