package polycube.polycard.card;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import polycube.polycard.manager.CardManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record Card(CardType type, Rarity rarity) {
    private static final Map<Card, ItemStackTemplate> itemStackCache = new HashMap<>();

    public Component getFormatedName() {
        return ComponentUtils.wrapInSquareBrackets(
                Component.literal(type.getName()).withStyle(
                        s -> s.withHoverEvent(
                                new HoverEvent.ShowItem(getItemTemplate())
                        )
                )
        ).withStyle(rarity.getColor());
    }

    public String getDisplayName() {
        return rarity.getName() + " " + type.getName();
    }

    public ItemStackTemplate getItemTemplate() {
        return itemStackCache.computeIfAbsent(this, card -> ItemStackTemplate.fromNonEmptyStack(CardManager.createCardItem(card)));
    }

    public ItemStack getItem() {
        return getItemTemplate().create();
    }

    public String getId() {
        return rarity.getId() + "_" + type.getId();
    }

    public static Optional<Card> fromId(String id) {
        String[] parts = id.split("_", 2);
        if (parts.length != 2) {
            return Optional.empty();
        }
        var rarity = Rarity.fromId(parts[0]);
        var type = CardType.fromId(parts[1]);
        if (rarity.isEmpty() || type.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Card(type.get(), rarity.get()));
    }
}
