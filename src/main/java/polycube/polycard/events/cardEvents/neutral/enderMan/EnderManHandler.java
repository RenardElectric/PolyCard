package polycube.polycard.events.cardEvents.neutral.enderMan;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.Rarity;
import polycube.polycard.events.ItemUseEvents;
import polycube.polycard.manager.CardManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnderManHandler {

    private static final int RESISTANCE_EFFECT_DURATION = 40;
    private static final int RESISTANCE_EFFECT_AMPLIFIER = 0;

    private static final List<ResourceKey<Biome>> endBiomes = new ArrayList<>(
            Arrays.asList(Biomes.THE_END, Biomes.END_BARRENS, Biomes.END_HIGHLANDS, Biomes.END_MIDLANDS, Biomes.SMALL_END_ISLANDS)
    );

    public static void registerEnderManCardEvents(CardManager cardManager) {
        ItemUseEvents.registerItemUseEvents(
                (player, world, hand) -> onEnderPearlUsed(cardManager, player, world, hand)
        );

        PolyCard.runTaskTimer(0, 20, server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                var pos = player.getOnPos();
                //noinspection resource
                var biome = player.level().getBiome(pos);
                if (biome.is(endBiomes::contains)) {
                    if (cardManager.getStorage().hasCardOrRarer(player, new Card(CardType.ENDERMAN, Rarity.LEGENDARY))) {
                        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, RESISTANCE_EFFECT_DURATION, RESISTANCE_EFFECT_AMPLIFIER, true, true));
                    }
                }
            }
        });
    }

    private static InteractionResult onEnderPearlUsed(CardManager cardManager, ServerPlayer player, Level world, InteractionHand hand) {
        var itemStack = player.getItemInHand(hand);
        if (itemStack.getItem() == Items.ENDER_PEARL) {
            if (cardManager.getStorage().hasCardOrRarer(player, new Card(CardType.ENDERMAN, Rarity.RARE))) {
                PolyCard.runLater(1, _ -> {
                    var cooldowns = player.getCooldowns();
                    cooldowns.removeCooldown(cooldowns.getCooldownGroup(itemStack));
                });

            }
        }

        return InteractionResult.PASS;
    }
}
