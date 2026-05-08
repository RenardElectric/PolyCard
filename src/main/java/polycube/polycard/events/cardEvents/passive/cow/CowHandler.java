package polycube.polycard.events.cardEvents.passive.cow;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.card.Rarity;
import polycube.polycard.events.callBacks.ItemConsumedEventCallback;
import polycube.polycard.manager.CardManager;

import java.util.*;

public class CowHandler {
    private static final long STILL_DELAY_MS = 500L;
    private static final int REGEN_HEALTH_GAIN = 8;
    private static final int REGEN_EFFECT_DURATION = 50;
    private static final int REGEN_EFFECT_AMPLIFIER = 0;
    private static final int RESISTANCE_EFFECT_DURATION = 80;
    private static final int RESISTANCE_EFFECT_AMPLIFIER = 0;
    private static final int RESISTANCE_DISTANCE_SQUARED = 25;

    private static final List<Holder<MobEffect>> DEBUFFS = List.of(
            MobEffects.WEAKNESS, MobEffects.MINING_FATIGUE, MobEffects.POISON,
            MobEffects.WITHER, MobEffects.HUNGER, MobEffects.UNLUCK, MobEffects.SLOWNESS, // TODO: No way to get the unluck effect
            MobEffects.BAD_OMEN, MobEffects.INFESTED, MobEffects.OOZING, MobEffects.WEAVING,
            MobEffects.WIND_CHARGED, MobEffects.BLINDNESS, MobEffects.DARKNESS, MobEffects.NAUSEA
    );

    private static final List<Holder<MobEffect>> BUFFS = List.of(
            MobEffects.STRENGTH, MobEffects.HASTE, MobEffects.ABSORPTION,
            MobEffects.REGENERATION, MobEffects.LUCK, MobEffects.SPEED, MobEffects.FIRE_RESISTANCE, // TODO: I do not think that the luck effect even works
            MobEffects.RESISTANCE, MobEffects.WATER_BREATHING, MobEffects.NIGHT_VISION
    );

    private static final Set<ServerPlayer> stillPlayers = new HashSet<>();
    private static final Map<UUID, Vec3> lastLocations = new HashMap<>();
    private static final Map<UUID, Long> lastMoveTimes = new HashMap<>();

    public static void registerCowCardEvents(CardManager cardManager) {
        ItemConsumedEventCallback.EVENT.register(
                (player, itemStack) -> onBucketUsed(cardManager, player, itemStack)
        );

        PolyCard.runTaskTimer(0, 20, server -> {
            // Regen when still
            long now = System.currentTimeMillis();
            var storage = cardManager.getStorage();
            var playersOnline = server.getPlayerList().getPlayers();
            for (ServerPlayer player : playersOnline) {

                var hasCard = storage.hasCardOrRarer(player, new Card(CardType.COW, Rarity.UNCOMMON));
                var pos = player.getOnPos();
                //noinspection resource
                var biome = player.level().getBiome(pos);
                if (!hasCard || !biome.is(Biomes.PLAINS)) continue;

                UUID playerId = player.getUUID();
                Vec3 currentLocation = player.position();
                Vec3 previousLocation = lastLocations.put(playerId, currentLocation);
                if (previousLocation == null || previousLocation.distanceToSqr(currentLocation) > 0.0001D) {
                    lastMoveTimes.put(playerId, now);
                    stillPlayers.remove(player);
                    continue;
                }
                long lastMoveTime = lastMoveTimes.getOrDefault(playerId, now);
                if (now - lastMoveTime >= STILL_DELAY_MS) {
                    stillPlayers.add(player);
                }
            }
            stillPlayers.removeIf(player -> !playersOnline.contains(player));
            stillPlayers.forEach(player -> {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_EFFECT_DURATION, REGEN_EFFECT_AMPLIFIER, true, true));
            });

            // Near Cow Card resistance
            for (Player player :  playersOnline.stream().filter(p -> cardManager.getStorage().hasCardOrRarer(p, new Card(CardType.COW, Rarity.RARE))).toList()) {
                boolean nearCowCard = playersOnline.stream()
                        .filter(p -> !p.equals(player))
                        .anyMatch(p -> cardManager.getStorage().hasCardOrRarer(p, new Card(CardType.COW, Rarity.RARE)) && p.position().distanceToSqr(player.position()) <= RESISTANCE_DISTANCE_SQUARED);
                if (nearCowCard) {
                    player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, RESISTANCE_EFFECT_DURATION, RESISTANCE_EFFECT_AMPLIFIER, true, true));
                }
            }
        });
    }

    private static InteractionResult onBucketUsed(CardManager cardManager, ServerPlayer player, ItemStack itemStack) {
        if (itemStack.getItem() == Items.MILK_BUCKET) {
            if (cardManager.getStorage().hasCardOrRarer(player, new Card(CardType.COW, Rarity.EPIC))) {
                var random = new Random();
                List<MobEffectInstance> effectsGained = new ArrayList<>();
                for (MobEffectInstance effect : player.getActiveEffects()) {
                    if (DEBUFFS.contains(effect.getEffect())) {
                        Holder<MobEffect> buffType = BUFFS.get(random.nextInt(BUFFS.size()));
                        effectsGained.add(new MobEffectInstance(buffType, effect.getDuration(), effect.getAmplifier(), true, true)); // TODO: Not sure about the amplifier part, because of bad omen 5...
                    }
                }

                PolyCard.runLater(1, _ -> {
                    for (MobEffectInstance effect : effectsGained) {
                        player.addEffect(effect);
                    }
                });
            }

            if (cardManager.getStorage().hasCardOrRarer(player, new Card(CardType.COW, Rarity.LEGENDARY))) {
                player.setHealth(player.getHealth() + REGEN_HEALTH_GAIN);
            }
        }

        return InteractionResult.PASS;
    }
}
