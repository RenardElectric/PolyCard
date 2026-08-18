package polycube.polycard.gui;

import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.CardEventCallback;
import polycube.polycard.utils.Helpers;

import java.util.*;

/// Server-side GUI for viewing and editing a player's equipped cards.
public final class EquipmentGUI extends SimpleGui {
    private static final Component TITLE = Component.literal("七ㇺ十").withStyle(ChatFormatting.WHITE)
            .append(Component.literal("✦༺ ").withStyle(ChatFormatting.DARK_RED))
            .append(Component.literal("Equipped Cards").withStyle(style ->
                    style.withColor(ChatFormatting.BLACK).withUnderlined(true)))
            .append(Component.literal(" ༻✦").withStyle(ChatFormatting.DARK_RED));

    private static final Map<UUID, Set<EquipmentGUI>> OPEN_GUIS = new HashMap<>();

    static {
        CardEventCallback.EQUIPPED.register((player, _) -> markOpenGuisDirty(player));
        CardEventCallback.UNEQUIPPED.register((player, _) -> markOpenGuisDirty(player));
    }

    private final ServerPlayer targetPlayer;
    private final PlayerData playerData;
    private final EquipmentContainer container;
    private boolean dirty;

    private EquipmentGUI(ServerPlayer viewer, ServerPlayer targetPlayer) {
        super(MenuType.HOPPER, viewer, false);
        this.targetPlayer = targetPlayer;
        this.playerData = PolyCard.storage().getPlayerData(targetPlayer);
        this.container = new EquipmentContainer();

        setTitle(TITLE);
        for (int slot = 0; slot < PlayerData.MAX_EQUIPPED_CARDS; slot++) {
            setSlot(slot, createEquipmentSlot(slot));
        }
    }

    /// Opens a player's own equipment manager.
    public static void openEquipmentGUI(ServerPlayer player) {
        openEquipmentGUI(player, player);
    }

    /// Opens an equipment manager where the viewer edits targetPlayer's cards.
    public static void openEquipmentGUI(ServerPlayer viewer, ServerPlayer targetPlayer) {
        var gui = new EquipmentGUI(viewer, targetPlayer);
        if (gui.open()) {
            gui.register();
            Helpers.debug("{} opened the equipment manager for {}", viewer.getName().getString(), targetPlayer.getName().getString());
        }
    }

    @Override
    public void onTick() {
        refreshIfDirty();
    }

    @Override
    public void onRemoved() {
        unregister();
    }

    private static void markOpenGuisDirty(ServerPlayer player) {
        var openGuis = OPEN_GUIS.get(player.getUUID());
        if (openGuis != null) {
            openGuis.forEach(EquipmentGUI::markDirty);
        }
    }

    private void markDirty() {
        dirty = true;
    }

    private void refreshIfDirty() {
        if (dirty) {
            dirty = false;
            container.syncFromPlayerData();
        }
    }

    private void register() {
        OPEN_GUIS.computeIfAbsent(targetPlayer.getUUID(), _ -> new HashSet<>()).add(this);
    }

    private void unregister() {
        var targetId = targetPlayer.getUUID();
        var openGuis = OPEN_GUIS.get(targetId);
        if (openGuis == null) {
            return;
        }

        openGuis.remove(this);
        if (openGuis.isEmpty()) {
            OPEN_GUIS.remove(targetId);
        }
    }

    private Slot createEquipmentSlot(int slot) {
        return new Slot(container, slot, 0, 0) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return EquipmentGUI.this.container.canPlaceCard(getContainerSlot(), itemStack);
            }
        };
    }

    private final class EquipmentContainer extends SimpleContainer {
        private EquipmentContainer() {
            super(PlayerData.MAX_EQUIPPED_CARDS);
            syncFromPlayerData();
        }

        @Override
        public ItemStack getItem(int slot) {
            refreshIfDirty();
            return super.getItem(slot);
        }

        @Override
        public void setChanged() {
            var displayedCards = items.stream()
                    .map(Card::getCard)
                    .flatMap(Optional::stream)
                    .toList();

            PlayerData.setEquippedCards(targetPlayer, displayedCards).mapOrElse(
                    change -> {
                        change.unequipped().forEach(card -> {
                            Helpers.debug("{} unequipped card {} for {}", getPlayer(), card, targetPlayer);
                            Helpers.playSound(getPlayer(), SoundEvents.BUNDLE_REMOVE_ONE);
                        });
                        change.equipped().forEach(card -> {
                            Helpers.debug("{} equipped card {} for {}", getPlayer(), card, targetPlayer);
                            Helpers.playSound(getPlayer(), SoundEvents.BUNDLE_INSERT);
                        });
                        markDirty();
                        return true;
                    },
                    error -> {
                        Helpers.SendFailure(getPlayer(), Component.literal("Failed to update equipment: " + error.message()));
                        markDirty();
                        return false;
                    }
            );
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        private void syncFromPlayerData() {
            var cardsToPlace = new EnumMap<CardType, Card>(CardType.class);
            for (var card : playerData.getEquippedCards()) {
                cardsToPlace.put(card.cardType(), card);
            }

            for (int slot = 0; slot < items.size(); slot++) {
                var item = items.get(slot);
                var displayedCard = Card.getCard(item).orElse(null);
                if (displayedCard == null) {
                    if (!item.isEmpty()) {
                        items.set(slot, ItemStack.EMPTY);
                    }
                    continue;
                }

                var equippedCard = cardsToPlace.remove(displayedCard.cardType());
                if (equippedCard == null) {
                    items.set(slot, ItemStack.EMPTY);
                } else if (!equippedCard.equals(displayedCard)) {
                    items.set(slot, equippedCard.asItem());
                }
            }

            for (var card : cardsToPlace.values()) {
                var emptySlot = firstEmptySlot();
                if (emptySlot == -1) {
                    break;
                }
                items.set(emptySlot, card.asItem());
            }
        }

        private boolean canPlaceCard(int slot, ItemStack itemStack) {
            var viewer = getPlayer();
            var card = Card.getCard(itemStack).orElse(null);
            if (card == null) {
                if (!itemStack.isEmpty()) {
                    Helpers.SendFailure(viewer, Component.literal("Failed to equip card: " + itemStack.getHoverName().getString() + " is not a card."));
                    Helpers.debug("Failed to equip card for {}: {} is not a card.", viewer.getName().getString(), itemStack.getHoverName().getString());
                }
                return false;
            }

            var proposedCards = new ArrayList<Card>();
            for (int index = 0; index < items.size(); index++) {
                if (index == slot) {
                    proposedCards.add(card);
                } else {
                    Card.getCard(items.get(index)).ifPresent(proposedCards::add);
                }
            }

            return playerData.canSetEquippedCards(proposedCards).mapOrElse(
                    _ -> true,
                    error -> {
                        var message = error.message();
                        Helpers.SendFailure(viewer, Component.literal("Failed to equip card: " + message));
                        Helpers.debug("Failed to equip card for {}: {}", viewer.getName().getString(), message);
                        return false;
                    }
            );
        }

        private int firstEmptySlot() {
            for (int slot = 0; slot < items.size(); slot++) {
                if (items.get(slot).isEmpty()) {
                    return slot;
                }
            }
            return -1;
        }
    }
}
