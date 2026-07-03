package polycube.polycard.cardEffects.hostile;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import polycube.polycard.card.Card;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.CardEventCallback;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.ItemDurabilityChangeEventCallback;

import java.util.List;

public class EnderDragonEffects
        extends CardEffects
        implements ItemDurabilityChangeEventCallback, EntityHurtEventCallback,
        ServerEntityEvents.EquipmentChange, CardEventCallback.CardEquipEvent, CardEventCallback.CardUnequipEvent
{

    public static final List<Item> chestplates = List.of(
            Items.LEATHER_CHESTPLATE, Items.IRON_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE,
            Items.COPPER_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.DIAMOND_CHESTPLATE,
            Items.NETHERITE_CHESTPLATE
    );

    private static final DataComponentPatch APPLY_GLIDER_PATCH = DataComponentPatch.builder().set(DataComponents.GLIDER, Unit.INSTANCE).build();
    private static final DataComponentPatch REMOVE_GLIDER_PATCH = DataComponentPatch.builder().remove(DataComponents.GLIDER).build();

    @Override
    public int onDurabilityChange(ServerLevel level, @Nullable ServerPlayer player, ItemStack itemStack, int amount) {
        if (player != null && PlayerData.hasCardOrRarer(player, cardType, RarityLevel.RARE)) {
            if (itemStack.getComponents().has(DataComponents.GLIDER)) {
                return 0;
            }
        }
        return amount;
    }

    @Override
    public InteractionResult onEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, MutableFloat damage) {
        if (entity instanceof ServerPlayer player) {
            if (PlayerData.hasCardOrRarer(player, cardType, RarityLevel.EPIC)) {
                if (source.is(DamageTypes.FLY_INTO_WALL) || (source.is(DamageTypes.FALL) && player.isFallFlying())) {
                    return InteractionResult.FAIL;
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onCardEquip(ServerPlayer player, Card card) {
        if (card.cardType() == cardType && card.rarityLevel().isAtLeast(RarityLevel.LEGENDARY)) {
            applyGliderComponent(player.getItemBySlot(EquipmentSlot.CHEST));
        }
    }

    @Override
    public void onCardUnequip(ServerPlayer player, Card card) {
        if (card.cardType() == cardType) {
            removeGliderComponent(player.getItemBySlot(EquipmentSlot.CHEST));
        }
    }

    @Override
    public void onChange(@NonNull LivingEntity livingEntity, @NonNull EquipmentSlot equipmentSlot, @NonNull ItemStack previousStack, @NonNull ItemStack currentStack) {
        if (livingEntity instanceof ServerPlayer player) {
            if (equipmentSlot == EquipmentSlot.CHEST) {
                if (PlayerData.hasCardOrRarer(player, cardType, RarityLevel.LEGENDARY)) {
                    applyGliderComponent(currentStack);
                } else {
                    removeGliderComponent(currentStack);
                }
            }
        }
    }

    private static void applyGliderComponent(ItemStack stack) {
        if (stack == ItemStack.EMPTY) return;
        if (!chestplates.contains(stack.getItem())) return;
        var components = stack.getComponents();
        if (!components.has(DataComponents.GLIDER)) {
            stack.applyComponents(APPLY_GLIDER_PATCH);
        }
    }

    private static void removeGliderComponent(ItemStack stack) {
        if (stack == ItemStack.EMPTY) return;
        if (!chestplates.contains(stack.getItem())) return;
        var components = stack.getComponents();
        if (components.has(DataComponents.GLIDER)) {
            stack.applyComponents(REMOVE_GLIDER_PATCH);
        }
    }
}
