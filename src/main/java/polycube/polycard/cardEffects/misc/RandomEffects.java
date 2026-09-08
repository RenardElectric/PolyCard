package polycube.polycard.cardEffects.misc;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jspecify.annotations.Nullable;
import polycube.polycard.PolyCard;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;
import polycube.polycard.utils.EffectHelpers;
import polycube.polycard.utils.Helpers;

import static polycube.polycard.utils.Helpers.decimalFormat;

public class RandomEffects extends CardEffects implements ServerTickEvents.EndTick, PlayerTickEventCallback {

    public static final int TICK_INTERVAL = 20 * 5;

    private static final Effect[] RANDOM_EFFECTS = new Effect[]{
            new RandomEffect(),
            new RandomAttribute(),
    };

    private long tickCounter = 0;
    private final PlayerState<Effect> randomEffect = new PlayerState<>();

    @Override
    public void onEndTick(MinecraftServer server) {
        tickCounter++;
    }

    @Override
    protected void onPlayerStateClearing(ServerPlayer player) {
        var card = revokeRandomEffect(player);
        if (card != null) {
            PolyCard.LOGGER.debug(
                    "Revoked temporary random effect grant {} from {} while clearing player state",
                    card, player.getName().getString()
            );
        }
    }

    @Override
    protected void onRuntimeClearing() {
        tickCounter = 0;
    }

    @Override
    public void onPlayerTick(MinecraftServer server, ServerPlayer player) {
        var cardRarityLevel = equippedRarityLevel(player);
        if (cardRarityLevel.isPresent()) {
            var playerEffect = randomEffect.get(player);
            if (playerEffect != null && playerEffect.eachTick()) playerEffect.apply(player);

            if (tickCounter % TICK_INTERVAL == 0) {
                Effect newEffect = RANDOM_EFFECTS[player.getRandom().nextInt(RANDOM_EFFECTS.length)];
                var previousEffect = replaceRandomEffect(player, newEffect);
                player.sendSystemMessage(
                        Component.literal("You have been granted a random effect for " + decimalFormat(TICK_INTERVAL/1200.0) + " minutes: ")
                                .withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(newEffect.name()).withStyle(ChatFormatting.AQUA))
                );
                if (previousEffect == null) {
                    PolyCard.LOGGER.debug("Granted {} temporary random effect {}", player.getName().getString(), newEffect.name());
                } else {
                    PolyCard.LOGGER.debug("Replaced {}'s temporary random effect {} with {}", player.getName().getString(), previousEffect.name(), newEffect.name());
                }
            }
        } else {
            var card = revokeRandomEffect(player);
            if (card != null) {
                PolyCard.LOGGER.debug(
                        "Revoked temporary random effect grant {} from {} after the Random card was removed",
                        card, player.getName().getString()
                );
            }
        }
    }

    private @Nullable Effect revokeRandomEffect(ServerPlayer player) {
        return replaceRandomEffect(player, null);
    }

    private @Nullable Effect replaceRandomEffect(ServerPlayer player, @Nullable Effect replacement) {
        var previous = randomEffect.get(player);
        if (previous == null && replacement == null) return null;

        if (replacement == null) randomEffect.remove(player);
        else randomEffect.put(player, replacement);

        if (previous != null) previous.remove(player);
        if (replacement != null) replacement.apply(player);
        return previous;
    }


    private interface Effect {
        String name();
        boolean eachTick();
        void apply(ServerPlayer player);
        void remove(ServerPlayer player);
    }

    private static class RandomEffect implements Effect {

        private @Nullable Holder<MobEffect> effect;
        private int amplifier;
        private String operationName = "Unknown";

        @Override
        public void apply(ServerPlayer player) {
            if (effect == null) {
                effect = BuiltInRegistries.MOB_EFFECT.getRandom(player.getRandom()).orElse(null);
                if (effect == null) return;
                amplifier = player.getRandom().nextInt(5);
                operationName = " " + (amplifier + 1);
            }
            EffectHelpers.refreshPersistentEffect(player, effect, amplifier);
        }

        @Override
        public void remove(ServerPlayer player) {
            effect = null;
            amplifier = 0;
            operationName = "Unknown";
        }

        @Override
        public String name() {
            if (effect == null) return operationName;
            var id = BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
            if (id == null) return operationName;
            return Helpers.identifierToTitleCase(id.getPath()) + " " + operationName;
        }

        @Override
        public boolean eachTick() {return true;}
    }

    private static class RandomAttribute implements Effect {

        private String operationName = "Unknown";
        @SuppressWarnings("NullableProblems")
        private final Multimap<Holder<Attribute>, AttributeModifier> map = HashMultimap.create();

        @Override
        public void apply(ServerPlayer player) {
            if (map.isEmpty()) {
                var attribute = BuiltInRegistries.ATTRIBUTE.getRandom(player.getRandom());
                if (attribute.isEmpty()) return;
                var modifier = Math.clamp(player.getRandom().nextGaussian() + 1, -1, 5);
                operationName += " x" + Helpers.decimalFormat(1 + modifier);
                map.put(attribute.get(), new AttributeModifier(Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, "random_attribute"), modifier, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            player.getAttributes().addTransientAttributeModifiers(map);
        }
        @Override
        public void remove(ServerPlayer player) {
            player.getAttributes().removeAttributeModifiers(map);
            map.clear();
            operationName = "Unknown";
        }

        @Override
        public String name() {
            if (map.isEmpty()) return operationName;
            var attribute = map.keySet().stream().findFirst().map(Holder::value).orElse(null);
            if (attribute == null) return operationName;
            var id = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
            if (id == null) return operationName;
            var path = id.getPath();
            return Helpers.identifierToTitleCase(path) + " " + operationName;
        }

        @Override
        public boolean eachTick() {return false;}
    }
}
