package polycube.polycard.cardEffects.misc;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.CardEventCallback;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;
import polycube.polycard.events.callBacks.GetBedRuleEventCallback;
import polycube.polycard.utils.EffectHelpers;

import java.util.Optional;

public class NetherEffects extends CardEffects implements PlayerTickEventCallback, GetBedRuleEventCallback, CardEventCallback.CardEquipEvent, CardEventCallback.CardUnequipEvent {
    @Override
    public void onPlayerTick(MinecraftServer server, ServerPlayer player) {
        if (hasCardOrRarer(player, RarityLevel.LEGENDARY)) {
            EffectHelpers.refreshPersistentEffect(player, MobEffects.FIRE_RESISTANCE, 0);
        }
    }

    @Override
    public BedRule getBedRule(Player player, BedRule original) {
        if (player instanceof ServerPlayer serverPlayer && hasCardOrRarer(serverPlayer, RarityLevel.LEGENDARY)) {
            if (player.level().dimension().equals(Level.NETHER)) {
                return new BedRule(BedRule.Rule.NEVER, BedRule.Rule.ALWAYS, false, Optional.empty());
            } else if (player.level().dimension().equals(Level.OVERWORLD)) {
                return new BedRule(BedRule.Rule.NEVER, BedRule.Rule.NEVER, false, Optional.of(Component.literal("You cannot sleep and set your respawn point in the Overworld while you have a Legendary Nether Card.")));
            }
        }
        return original;
    }

    @Override
    public void onCardEquip(ServerPlayer player, Card card) {
        if (card.cardType() == cardType()) {
            var config = player.getRespawnConfig();
            if (config != null) {
                var dimension = config.respawnData().dimension();
                if (dimension.equals(Level.OVERWORLD)) {
                    player.setRespawnPosition(null, false);
                    player.sendSystemMessage(Component.literal("Your respawn point in the overworld has been removed because you equipped a Legendary Nether Card."));
                    PolyCard.LOGGER.debug(
                            "Removed {}'s Overworld respawn point after equipping {}",
                            player.getName().getString(), card
                    );
                }
            }
        }
    }

    @Override
    public void onCardUnequip(ServerPlayer player, Card card) {
        if (card.cardType() == cardType()) {
            var config = player.getRespawnConfig();
            if (config != null) {
                var data = config.respawnData();
                var dimension = data.dimension();
                var respawnLevel = player.level().getServer().getLevel(dimension);
                var isBed = respawnLevel != null && respawnLevel.getBlockState(data.pos()).getBlock() instanceof BedBlock;
                if (dimension.equals(Level.NETHER) && isBed) {
                    player.setRespawnPosition(null, false);
                    player.sendSystemMessage(Component.literal("Your respawn point in the nether has been removed because you unequipped a Legendary Nether Card."));
                    PolyCard.LOGGER.debug(
                            "Removed {}'s Nether respawn point after unequipping {}",
                            player.getName().getString(), card
                    );
                }
            }
        }
    }
}
