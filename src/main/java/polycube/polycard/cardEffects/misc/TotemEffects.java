package polycube.polycard.cardEffects.misc;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import polycube.polycard.PolyCard;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.data.PlayerData;

public class TotemEffects extends CardEffects implements ServerLivingEntityEvents.AllowDeath, ServerLivingEntityEvents.AfterDamage {
    private final PlayerState<ItemStack> offHandBackups = new PlayerState<>();

    @Override
    public boolean allowDeath(LivingEntity entity, DamageSource damageSource, float damageAmount) {
        if (entity instanceof ServerPlayer player) {

            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack itemStack = player.getItemInHand(hand);
                if (itemStack.get(DataComponents.DEATH_PROTECTION) != null) {
                    return true;
                }
            }

            equippedCard(player).ifPresent(card -> {
                PlayerData.downgradeCard(player, card).mapOrElse(
                        _ -> true,
                        error -> {
                            PolyCard.LOGGER.warn(
                                    "Failed to downgrade {} while protecting {} from death: {}",
                                    card, player.getName().getString(), error.message()
                            );
                            return false;
                        }
                );

                offHandBackups.put(player, player.getItemInHand(InteractionHand.OFF_HAND));
                player.setItemInHand(InteractionHand.OFF_HAND, Items.TOTEM_OF_UNDYING.getDefaultInstance());
                PolyCard.LOGGER.debug(
                        "{} activated {} Totem-card protection; temporarily replaced the off-hand item",
                        player.getName().getString(), card.rarityLevel()
                );
            });

        }
        return true;
    }

    @Override
    public void afterDamage(LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (entity instanceof ServerPlayer player) {
            restoreOffHand(player);
        }
    }

    @Override
    protected void onPlayerStateClearing(ServerPlayer player) {
        restoreOffHand(player);
    }

    private void restoreOffHand(ServerPlayer player) {
        var offHandBackup = offHandBackups.remove(player);
        if (offHandBackup != null) {
            player.setItemInHand(InteractionHand.OFF_HAND, offHandBackup);
            PolyCard.LOGGER.debug(
                    "Restored {}'s off-hand item after Totem-card protection",
                    player.getName().getString()
            );
        }
    }
}
