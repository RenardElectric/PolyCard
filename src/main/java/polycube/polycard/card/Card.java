package polycube.polycard.card;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.sgui.api.elements.ItemStackBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import org.jspecify.annotations.NonNull;
import polycube.polycard.PolyCard;
import polycube.polycard.manager.CardManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public record Card(CardType cardType, RarityType rarityType) {

    public static final Codec<Card> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CardType.CODEC.fieldOf("cardType").forGetter(Card::cardType),
            RarityType.CODEC.fieldOf("rarityType").forGetter(Card::rarityType)
    ).apply(instance, Card::new));

    private static final String CARD_TYPE_KEY = "cardType";
    private static final String RARITY_TYPE_KEY = "rarityType";
    private static final Map<Card, ItemStackTemplate> itemStackCache = new HashMap<>();

    public ItemStack asItem() {
        return getItemTemplate().create();
    }

    private ItemStackTemplate getItemTemplate() {
        return itemStackCache.computeIfAbsent(this, Card::createCardItemTemplate);
    }

    public Component getFormatedName() {
        return ComponentUtils.wrapInSquareBrackets(
                Component.literal(cardType.toString()).withStyle(
                        s -> s.withHoverEvent(
                                new HoverEvent.ShowItem(getItemTemplate())
                        )
                )
        ).withStyle(rarityType.color());
    }

    @Override
    public @NonNull String toString() {
        return rarityType + " " + cardType;
    }

    // Helper methods to manipulate card ItemStacks
    private static ItemStackTemplate createCardItemTemplate(Card card) {
        var rarity = card.rarityType();
        var cardType = card.cardType();

        var customDataTag = new CompoundTag();
        var polyCardTag = new CompoundTag();
        polyCardTag.putString(CARD_TYPE_KEY, cardType.getSerializedName());
        polyCardTag.putString(RARITY_TYPE_KEY, rarity.getSerializedName());
        customDataTag.put(PolyCard.MOD_ID, polyCardTag);

        var components = DataComponentPatch.builder()
                .set(DataComponents.ITEM_NAME, Component.literal(card.toString()).withStyle(rarity.color()))
                .set(DataComponents.LORE, new ItemLore(cardType.getDescriptions(rarity)))
                .set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, cardType.isEnchanted(rarity))
                .set(DataComponents.CUSTOM_DATA, CustomData.of(customDataTag));

        // TODO: Temporary
        var item = switch (rarity) {
            case COMMON -> Items.GRAY_DYE;
            case UNCOMMON -> Items.GREEN_DYE;
            case RARE -> Items.BLUE_DYE;
            case EPIC -> Items.PURPLE_DYE;
            case LEGENDARY -> Items.ORANGE_DYE;
        };

        return new ItemStackTemplate(item, components.build());
    }

    public static boolean isCard(ItemStack item) {
        return getCardType(item).isPresent() && getCardRarity(item).isPresent();
    }

    public static Optional<Card> getCard(ItemStack item) {
        var cardType = getCardType(item);
        var rarity = getCardRarity(item);
        if (cardType.isPresent() && rarity.isPresent()) {
            return Optional.of(new Card(cardType.get(), rarity.get()));
        }
        return Optional.empty();
    }

    public static Optional<CardType> getCardType(ItemStack item) {
        return getCardData(item, CARD_TYPE_KEY, CardType::deserialize);
    }

    public static Optional<RarityType> getCardRarity(ItemStack item) {
        return getCardData(item, RARITY_TYPE_KEY, RarityType::deserialize);
    }

    private static <T> Optional<T> getCardData(ItemStack item, String key, Function<String, Optional<T>> fromId) {
        var customData = item.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return Optional.empty();
        return customData.copyTag()
                .getCompound(PolyCard.MOD_ID)
                .flatMap(compoundTag -> compoundTag.getString(key)
                        .flatMap(fromId));
    }
}
