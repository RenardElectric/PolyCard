package polycube.polycard.cardEffects.hostile;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.phys.Vec3;
import polycube.polycard.PolyCard;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.ContainerClickEventCallback;
import polycube.polycard.events.callBacks.EntityAfterHurtEventCallback;
import polycube.polycard.events.callBacks.ItemUseEventCallback;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;
import polycube.polycard.gui.ItemShulkerBoxMenu;

public final class ShulkerEffects extends CardEffects implements PlayerTickEventCallback, EntityAfterHurtEventCallback, ItemUseEventCallback, ContainerClickEventCallback {
    public static final int STILL_DELAY = 10;
    public static final double STILL_ARMOR = 3;
    public static final double STILL_ARMOR_TOUGHNESS = 1;

    public static final float LEVITATION_ON_HIT_PROBABILITY = 0.2f;
    public static final int LEVITATION_DURATION = 20;
    public static final int LEVITATION_AMPLIFIER = 0;

    private static final Identifier ARMOR_MODIFIER_ID = Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, "shulker_still_armor");
    private static final Identifier ARMOR_TOUGHNESS_MODIFIER_ID = Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, "shulker_still_armor_toughness");
    private static final AttributeModifier ARMOR_MODIFIER = new AttributeModifier(ARMOR_MODIFIER_ID, STILL_ARMOR, AttributeModifier.Operation.ADD_VALUE);
    private static final AttributeModifier ARMOR_TOUGHNESS_MODIFIER = new AttributeModifier(ARMOR_TOUGHNESS_MODIFIER_ID, STILL_ARMOR_TOUGHNESS, AttributeModifier.Operation.ADD_VALUE);

    private final PlayerState<Vec3> lastLocations = new PlayerState<>();
    private final PlayerState<Integer> stillTicks = new PlayerState<>();

    @Override
    public void onPlayerTick(MinecraftServer server, ServerPlayer player) {
        if (!hasCardOrRarer(player, RarityLevel.RARE)) {
            if (lastLocations.remove(player) != null) {
                stillTicks.remove(player);
                setStillArmor(player, false);
            }
            return;
        }

        var currentLocation = player.position();
        var previousLocation = lastLocations.put(player, currentLocation);
        if (previousLocation == null || previousLocation.distanceToSqr(currentLocation) > 0.0001D) {
            stillTicks.remove(player);
            setStillArmor(player, false);
            return;
        }

        int ticks = stillTicks.getOrDefault(player, 0) + 1;
        stillTicks.put(player, ticks);
        if (ticks == STILL_DELAY) {
            setStillArmor(player, true);
        }
    }

    @Override
    protected void onPlayerStateClearing(ServerPlayer player) {
        setStillArmor(player, false);
    }

    private static void setStillArmor(ServerPlayer player, boolean active) {
        var armor = player.getAttribute(Attributes.ARMOR);
        var armorToughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (armor != null) {
            armor.removeModifier(ARMOR_MODIFIER_ID);
            if (active) armor.addTransientModifier(ARMOR_MODIFIER);
        }
        if (armorToughness != null) {
            armorToughness.removeModifier(ARMOR_TOUGHNESS_MODIFIER_ID);
            if (active) armorToughness.addTransientModifier(ARMOR_TOUGHNESS_MODIFIER);
        }
    }

    @Override
    public void afterEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, float damageDealt) {
        if (source.getEntity() instanceof ServerPlayer player
                && !entity.equals(player)
                && hasCardOrRarer(player, RarityLevel.EPIC)
                && level.getRandom().nextFloat() < LEVITATION_ON_HIT_PROBABILITY) {
            PolyCard.LOGGER.debug("{} triggered Epic Shulker levitation against {}", player.getName().getString(), entity.getName().getString());
            entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, LEVITATION_DURATION, LEVITATION_AMPLIFIER), player);
        }
    }

    @Override
    public InteractionResult onItemUse(ServerPlayer player, Level world, InteractionHand hand) {
        int inventorySlot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;
        if (!canOpenShulkerBox(player, inventorySlot)) {
            return InteractionResult.PASS;
        }
        openShulkerBox(player, inventorySlot);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onContainerClick(ServerPlayer player, AbstractContainerMenu menu, int slotId, int button, ContainerInput input) {
        // Only a plain right-click with an empty cursor in the player's own inventory screen.
        if (menu != player.inventoryMenu || input != ContainerInput.PICKUP || button != 1
                || !menu.getCarried().isEmpty() || !menu.isValidSlotIndex(slotId) || slotId < 0) {
            return InteractionResult.PASS;
        }
        var slot = menu.getSlot(slotId);
        int inventorySlot = slot.getContainerSlot();
        if (slot.container != player.getInventory() || !canOpenShulkerBox(player, inventorySlot)) {
            return InteractionResult.PASS;
        }

        // The click packet is still being processed, so the menu is swapped once it is done.
        var shulkerStack = slot.getItem();
        scheduler().runLater(0, _ -> {
            if (player.containerMenu == player.inventoryMenu
                    && player.inventoryMenu.getCarried().isEmpty()
                    && player.getInventory().getItem(inventorySlot) == shulkerStack
                    && canOpenShulkerBox(player, inventorySlot)) {
                openShulkerBox(player, inventorySlot);
            }
        });
        return InteractionResult.FAIL;
    }

    private boolean canOpenShulkerBox(ServerPlayer player, int inventorySlot) {
        if (!player.isAlive() || player.isSpectator() || !ItemShulkerBoxMenu.isSupportedInventorySlot(inventorySlot)) {
            return false;
        }
        var itemStack = player.getInventory().getItem(inventorySlot);
        return itemStack.getCount() == 1
                && itemStack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock
                && hasCardOrRarer(player, RarityLevel.LEGENDARY);
    }

    private void openShulkerBox(ServerPlayer player, int inventorySlot) {
        var itemStack = player.getInventory().getItem(inventorySlot);
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, _) -> ItemShulkerBoxMenu.create(containerId, inventory, inventorySlot),
                itemStack.getHoverName()
        ));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHULKER_BOX_OPEN, SoundSource.PLAYERS, 0.5F, 1.0F);
    }
}
