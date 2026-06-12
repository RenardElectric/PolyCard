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
import org.jspecify.annotations.Nullable;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.data.PlayerData;
import polycube.polycard.events.callBacks.CardEventCallback;
import polycube.polycard.events.callBacks.EntityHurtEventCallback;
import polycube.polycard.events.callBacks.ItemDurabilityChangeEventCallback;

import java.util.List;

public class EnderDragonEffects {
    public static final CardType CARD_TYPE = CardType.ENDER_DRAGON;

    public static final List<Item> chestplates = List.of(
            Items.LEATHER_CHESTPLATE, Items.IRON_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE,
            Items.COPPER_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.DIAMOND_CHESTPLATE,
            Items.NETHERITE_CHESTPLATE
    );

    private static final DataComponentPatch APPLY_GLIDER_PATCH = DataComponentPatch.builder().set(DataComponents.GLIDER, Unit.INSTANCE).build();
    private static final DataComponentPatch REMOVE_GLIDER_PATCH = DataComponentPatch.builder().remove(DataComponents.GLIDER).build();

    public static void register() {
        ItemDurabilityChangeEventCallback.EVENT.register(EnderDragonEffects::onDurabilityChange);
        EntityHurtEventCallback.EVENT.register(EnderDragonEffects::onPlayerHurt);
        CardEventCallback.EQUIPPED.register(EnderDragonEffects::onEquip);
        CardEventCallback.UNEQUIPPED.register(EnderDragonEffects::onUnequip);
        ServerEntityEvents.EQUIPMENT_CHANGE.register(EnderDragonEffects::onEquipmentChange);
    }

    private static int onDurabilityChange(ServerLevel level, @Nullable ServerPlayer player, ItemStack stack, int amount) {
        if (player != null && PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.RARE)) {
            if (stack.getComponents().has(DataComponents.GLIDER)) {
                return 0;
            }
        }
        return amount;
    }

    private static InteractionResult onPlayerHurt(LivingEntity entity, ServerLevel level, DamageSource source, MutableFloat damage) {
        if (entity instanceof ServerPlayer player) {
            if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.EPIC)) {
                if (source.is(DamageTypes.FLY_INTO_WALL) || (source.is(DamageTypes.FALL) && player.isFallFlying())) {
                    return InteractionResult.FAIL;
                }
            }
        }
        return InteractionResult.PASS;
    }

    private static void onEquip(ServerPlayer player, Card card) {
        if (card.cardType() == CARD_TYPE && card.rarityLevel().isAtLeast(RarityLevel.LEGENDARY)) {
            applyGliderComponent(player.getItemBySlot(EquipmentSlot.CHEST));
        }
    }

    private static void onUnequip(ServerPlayer player, Card card) {
        if (card.cardType() == CARD_TYPE) {
            removeGliderComponent(player.getItemBySlot(EquipmentSlot.CHEST));
        }
    }

    private static void onEquipmentChange(LivingEntity entity, EquipmentSlot slot, ItemStack previous, ItemStack next) {
        if (entity instanceof ServerPlayer player) {
            if (slot == EquipmentSlot.CHEST) {
                if (PlayerData.hasCardOrRarer(player, CARD_TYPE, RarityLevel.LEGENDARY)) {
                    applyGliderComponent(next);
                } else {
                    removeGliderComponent(next);
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
