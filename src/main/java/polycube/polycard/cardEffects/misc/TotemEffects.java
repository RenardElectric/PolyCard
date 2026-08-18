package polycube.polycard.cardEffects.misc;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;
import polycube.polycard.card.Card;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.data.PlayerData;

public class TotemEffects extends CardEffects implements ServerLivingEntityEvents.AllowDeath, ServerLivingEntityEvents.AfterDamage {
    private @Nullable ItemStack mainHand = null;

    @Override
    public boolean allowDeath(LivingEntity entity, DamageSource damageSource, float damageAmount) {
        if (entity instanceof ServerPlayer player) {

            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack itemStack = player.getItemInHand(hand);
                if (itemStack.get(DataComponents.DEATH_PROTECTION) != null) {
                    return true;
                }
            }

            var rarityLevel = equippedRarityLevel(player);
            if (rarityLevel != null) {
                var card = new Card(cardType(), rarityLevel);
                PlayerData.downgradeCard(player, card);

                mainHand = player.getItemInHand(InteractionHand.OFF_HAND);
                player.setItemInHand(InteractionHand.OFF_HAND, Items.TOTEM_OF_UNDYING.getDefaultInstance());
            }

        }
        return true;
    }

    @Override
    public void afterDamage(LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (entity instanceof ServerPlayer player) {
            if (mainHand != null) {
                player.setItemInHand(InteractionHand.OFF_HAND, mainHand);
                mainHand = null;
            }
        }
    }
}
