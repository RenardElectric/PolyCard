package polycube.polycard.card;

import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import org.jspecify.annotations.NonNull;
import polycube.polycard.PolyCard;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/// Represents a card with a specific type and rarity level,
/// which can be converted to an ItemStack for use in Minecraft.
public record Card(CardType cardType, RarityLevel rarityLevel) {

    public static final Codec<Card> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CardType.CODEC.fieldOf("cardType").forGetter(Card::cardType),
            RarityLevel.CODEC.fieldOf("rarityLevel").forGetter(Card::rarityLevel)
    ).apply(instance, Card::new));

    public static final int CARDS_FOR_NEXT_LEVEL = 20;

    private static final String CARD_TYPE_KEY = "cardType";
    private static final String RARITY_LEVEL_KEY = "rarityLevel";
    private static final Map<Card, ItemStackTemplate> itemStackCache = new HashMap<>();

    /// Converts this Card to an ItemStack that can be used in Minecraft,
    /// with appropriate custom data and display properties.
    ///
    /// @return an ItemStack representing this Card
    public ItemStack asItem() {
        return getItemTemplate().create();
    }

    /// Retrieves the ItemStackTemplate for this Card, using a cache to avoid redundant creation.
    /// If the template is not already cached, it will be created and stored in the cache.
    private ItemStackTemplate getItemTemplate() {
        // TODO: Still not sure if it is a good idea to make a cache for it
        return itemStackCache.computeIfAbsent(this, Card::createCardItemTemplate);
    }

    /// Returns a formatted Component representing the name of this Card,
    /// which includes the card type and rarity level, and shows item details on hover.
    ///
    /// @return a Component representing the formatted name of this Card
    public Component getFormattedName() {
        return ComponentUtils.wrapInSquareBrackets(
                Component.literal(toString()).withStyle(
                        s -> s.withHoverEvent(
                                new HoverEvent.ShowItem(getItemTemplate())
                        )
                )
        ).withStyle(rarityLevel.color());
    }

    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers() {
        return cardType.getAttributeModifiers(rarityLevel);
    }

    @Override
    public @NonNull String toString() {
        return rarityLevel + " " + cardType + " card";
    }

    // Helper methods to manipulate card ItemStacks

    /// Creates an ItemStackTemplate for a given Card,
    /// which includes custom data tags and display properties
    /// based on the card's type and rarity.
    ///
    /// @param card the Card for which to create the ItemStackTemplate
    /// @return an ItemStackTemplate representing the given Card
    private static ItemStackTemplate createCardItemTemplate(Card card) {
        var rarity = card.rarityLevel();
        var cardType = card.cardType();

        var cardTypeName = cardType.getSerializedName();
        var rarityName = rarity.getSerializedName();

        var customDataTag = new CompoundTag();
        var polyCardTag = new CompoundTag();
        polyCardTag.putString(CARD_TYPE_KEY, cardTypeName);
        polyCardTag.putString(RARITY_LEVEL_KEY, rarityName);
        customDataTag.put(PolyCard.MOD_ID, polyCardTag);

        var components = DataComponentPatch.builder()
                .set(DataComponents.ITEM_NAME, Component.literal(card.toString()).withStyle(rarity.color()))
                .set(DataComponents.LORE, new ItemLore(cardType.getDescriptions(rarity)))
                .set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, cardType.isEnchanted(rarity))
                .set(DataComponents.CUSTOM_DATA, CustomData.of(customDataTag))
                .set(DataComponents.MAX_STACK_SIZE, 64)
                .set(DataComponents.ITEM_MODEL, Identifier.parse(PolyCard.MOD_ID + ":" + cardTypeName + "/" + rarityName));

        return new ItemStackTemplate(Items.KNOWLEDGE_BOOK, components.build());
    }

    /// Checks if a given ItemStack represents a valid Card
    /// by verifying the presence of card type and rarity data.
    ///
    /// @param item the ItemStack to check
    /// @return true if the ItemStack represents a valid Card, false otherwise
    public static boolean isCard(ItemStack item) {
        return getCardType(item).isPresent() && getCardRarity(item).isPresent();
    }

    /// Retrieves a Card instance from a given ItemStack if it represents a valid Card,
    /// by extracting the card type and rarity data from the item's custom data.
    ///
    /// @param item the ItemStack from which to retrieve the Card
    /// @return an Optional containing the Card if the ItemStack is valid, or empty if not
    public static Optional<Card> getCard(ItemStack item) {
        var cardType = getCardType(item);
        var rarity = getCardRarity(item);
        if (cardType.isPresent() && rarity.isPresent()) {
            return Optional.of(new Card(cardType.get(), rarity.get()));
        }
        return Optional.empty();
    }

    /// Retrieves the CardType from a given ItemStack if it represents a valid Card,
    /// by extracting the card type data from the item's custom data.
    ///
    /// @param item the ItemStack from which to retrieve the CardType
    /// @return an Optional containing the CardType if the ItemStack is valid, or empty
    public static Optional<CardType> getCardType(ItemStack item) {
        return getCardData(item, CARD_TYPE_KEY, CardType::deserialize);
    }

    /// Retrieves the RarityType from a given ItemStack if it represents a valid Card,
    /// by extracting the rarity level data from the item's custom data.
    ///
    /// @param item the ItemStack from which to retrieve the RarityType
    public static Optional<RarityLevel> getCardRarity(ItemStack item) {
        return getCardData(item, RARITY_LEVEL_KEY, RarityLevel::deserialize);
    }

    /// A helper method to extract specific card data (such as card type or rarity) from an ItemStack's custom data,
    /// using a provided key and a deserialization function.
    private static <T> Optional<T> getCardData(ItemStack item, String key, Function<String, Optional<T>> fromId) {
        var customData = item.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return Optional.empty();
        return customData.copyTag()
                .getCompound(PolyCard.MOD_ID)
                .flatMap(compoundTag -> compoundTag.getString(key)
                        .flatMap(fromId));
    }
}
