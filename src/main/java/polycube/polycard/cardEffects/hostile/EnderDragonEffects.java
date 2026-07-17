package polycube.polycard.cardEffects.hostile;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.apache.commons.lang3.mutable.MutableFloat;
import polycube.polycard.card.Card;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.CardEventCallback;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.FallFlyingGliderWearEventCallback;
import polycube.polycard.utils.Helpers;

public class EnderDragonEffects
        extends CardEffects
        implements FallFlyingGliderWearEventCallback, EntityHurtEventCallback,
        ServerEntityEvents.EquipmentChange, CardEventCallback.CardEquipEvent, CardEventCallback.CardUnequipEvent,
        ServerPlayerEvents.Join, ServerPlayerEvents.AfterRespawn {
    private static final String OWNED_GLIDER_MARKER = "polycard:dragon_glider";

    @Override
    public boolean cancelGliderWear(ServerPlayer player, ItemStack glider, EquipmentSlot slot) {
        return PlayerData.hasCardOrRarer(player, cardType(), RarityLevel.RARE);
    }

    @Override
    public InteractionResult onEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, MutableFloat damage) {
        if (entity instanceof ServerPlayer player) {
            if (PlayerData.hasCardOrRarer(player, cardType(), RarityLevel.EPIC)) {
                if (source.is(DamageTypes.FLY_INTO_WALL) || (source.is(DamageTypes.FALL) && player.isFallFlying())) {
                    return InteractionResult.FAIL;
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onCardEquip(ServerPlayer player, Card card) {
        if (card.cardType() == cardType() && card.rarityLevel().isAtLeast(RarityLevel.LEGENDARY)) {
            removeOwnedGliderFromInventory(player);
            applyGliderComponent(player.getItemBySlot(EquipmentSlot.CHEST));
        }
    }

    @Override
    public void onCardUnequip(ServerPlayer player, Card card) {
        if (card.cardType() == cardType()) {
            removeGliderComponent(player.getItemBySlot(EquipmentSlot.CHEST));
            removeOwnedGliderFromInventory(player);
        }
    }

    @Override
    public void onChange(LivingEntity livingEntity, EquipmentSlot equipmentSlot, ItemStack previousStack, ItemStack currentStack) {
        if (livingEntity instanceof ServerPlayer player) {
            if (equipmentSlot == EquipmentSlot.CHEST) {
                removeOwnedGliderFromInventory(player);
                if (PlayerData.hasCardOrRarer(player, cardType(), RarityLevel.LEGENDARY)) {
                    applyGliderComponent(currentStack);
                } else {
                    removeGliderComponent(currentStack);
                }
            }
        }
    }

    private static void applyGliderComponent(ItemStack stack) {
        if (!isChestEquipment(stack)) return;
        if (!stack.has(DataComponents.GLIDER)) {
            stack.set(DataComponents.GLIDER, Unit.INSTANCE);
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(OWNED_GLIDER_MARKER, true));
            Helpers.debug("Applied temporary Dragon-card glider component to {}", stack.getHoverName().getString());
        }
    }

    @Override
    public void onJoin(ServerPlayer player) {
        synchronizeOwnedGlider(player);
    }

    @Override
    public void afterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        synchronizeOwnedGlider(newPlayer);
    }

    private static boolean removeGliderComponent(ItemStack stack) {
        if (!hasOwnedGlider(stack)) return false;
        stack.remove(DataComponents.GLIDER);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(OWNED_GLIDER_MARKER));
        Helpers.debug("Removed temporary Dragon-card glider component from {}", stack.getHoverName().getString());
        return true;
    }

    private static boolean isChestEquipment(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
    }

    private static boolean hasOwnedGlider(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.copyTag().getBooleanOr(OWNED_GLIDER_MARKER, false);
    }

    /// Removes stale marked components, then restores the live capability only when still eligible.
    private void synchronizeOwnedGlider(ServerPlayer player) {
        removeOwnedGliderFromInventory(player);
        if (PlayerData.hasCardOrRarer(player, cardType(), RarityLevel.LEGENDARY)) {
            applyGliderComponent(player.getItemBySlot(EquipmentSlot.CHEST));
        }
    }

    private static void removeOwnedGliderFromInventory(ServerPlayer player) {
        boolean inventoryChanged = false;

        for (var equipmentSlot : EquipmentSlot.VALUES) {
            inventoryChanged |= removeGliderComponent(player.getItemBySlot(equipmentSlot));
        }
        for (var stack : player.getInventory().getNonEquipmentItems()) {
            inventoryChanged |= removeGliderComponent(stack);
        }
        if (inventoryChanged) {
            player.getInventory().setChanged();
        }

        // Include the currently open container (chests, crafting tables, GUIs, and player slots).
        for (var slot : player.containerMenu.slots) {
            if (removeGliderComponent(slot.getItem())) {
                slot.setChanged();
            }
        }

        var carried = player.containerMenu.getCarried();
        if (removeGliderComponent(carried)) {
            player.containerMenu.setCarried(carried);
        }
    }
}
