package polycube.polycard.cardEffects.misc;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import polycube.polycard.card.Card;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.CardEventCallback;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;
import polycube.polycard.events.callBacks.GetBedRuleEventCallback;
import polycube.polycard.utils.EffectHelpers;

import java.util.Optional;

public class NetherEffects extends CardEffects implements PlayerTickEventCallback, GetBedRuleEventCallback, CardEventCallback.CardEquipEvent {
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
                return new BedRule(BedRule.Rule.NEVER, BedRule.Rule.ALWAYS, false, Optional.of(Component.literal("Respawn point set")));
            } else if (player.level().dimension().equals(Level.OVERWORLD)) {
                return new BedRule(BedRule.Rule.WHEN_DARK, BedRule.Rule.NEVER, false, Optional.of(Component.literal("You cannot sleep in the Overworld while you have a Legendary Nether card.")));
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
                    player.sendSystemMessage(Component.literal("Your respawn point in the overworld has been removed because you equipped a Legendary Nether card."));
                }
            }
        }
    }
}
