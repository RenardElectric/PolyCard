package polycube.polycard.card;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.NonNull;
import polycube.polycard.manager.CardManager;

import java.util.HashMap;
import java.util.Map;

public record Card(CardType type, Rarity rarity) {

    public static final Codec<Card> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CardType.CODEC.fieldOf("type").forGetter(Card::type),
            Rarity.CODEC.fieldOf("rarity").forGetter(Card::rarity)
    ).apply(instance, Card::new));

    private static final Map<Card, ItemStackTemplate> itemStackCache = new HashMap<>();

    public Component getFormatedName() {
        return ComponentUtils.wrapInSquareBrackets(
                Component.literal(type.toString()).withStyle(
                        s -> s.withHoverEvent(
                                new HoverEvent.ShowItem(getItemTemplate())
                        )
                )
        ).withStyle(rarity.color());
    }

    public ItemStack asItem() {
        return getItemTemplate().create();
    }

    private ItemStackTemplate getItemTemplate() {
        return itemStackCache.computeIfAbsent(this, card -> ItemStackTemplate.fromNonEmptyStack(CardManager.createCardItem(card)));
    }

    @Override
    public @NonNull String toString() {
        return rarity + " " + type;
    }
}
