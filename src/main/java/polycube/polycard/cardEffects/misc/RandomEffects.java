package polycube.polycard.cardEffects.misc;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jspecify.annotations.Nullable;
import polycube.polycard.PolyCard;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;
import polycube.polycard.utils.EffectHelpers;

import static polycube.polycard.utils.Helpers.decimalFormat;

public class RandomEffects extends CardEffects implements ServerTickEvents.EndTick, PlayerTickEventCallback {

    public static final int TICK_INTERVAL = 20 * 60 * 5;

    private static final RandomEffect[] RANDOM_EFFECTS = new RandomEffect[]{
            new Effect("Strength Boost", MobEffects.STRENGTH, 1),
            new Effect("Regeneration", MobEffects.REGENERATION, 1),
            new Effect("Invisibility", MobEffects.INVISIBILITY, 0),
            new Attribut("Size Increase", "scale_up", Attributes.SCALE, 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
            new Attribut("Size Decrease", "scale_down", Attributes.SCALE, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
    };

    private long tickCounter = 0;
    private final PlayerState<RandomEffect> randomEffect = new PlayerState<>();

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
                RandomEffect newEffect = RANDOM_EFFECTS[player.getRandom().nextInt(RANDOM_EFFECTS.length)];
                var previousEffect = replaceRandomEffect(player, newEffect);
                player.sendSystemMessage(
                        Component.literal("You have been granted a random effect for " + decimalFormat(TICK_INTERVAL/1200.0) + " minutes: ")
                                .withStyle(ChatFormatting.GREEN)
                                .append(newEffect.name())
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

    private @Nullable RandomEffect revokeRandomEffect(ServerPlayer player) {
        return replaceRandomEffect(player, null);
    }

    private @Nullable RandomEffect replaceRandomEffect(ServerPlayer player, @Nullable RandomEffect replacement) {
        var previous = randomEffect.get(player);
        if (previous == null && replacement == null) return null;

        if (replacement == null) randomEffect.remove(player);
        else randomEffect.put(player, replacement);

        if (replacement != previous) {
            if (previous != null) previous.remove(player);
            if (replacement != null) replacement.apply(player);
        }
        return previous;
    }


    private interface RandomEffect {
        String name();
        boolean eachTick();
        void apply(ServerPlayer player);
        void remove(ServerPlayer player);
    }

    private record Effect(String name, Holder<MobEffect> effect, int amplifier) implements RandomEffect {
        @Override
        public void apply(ServerPlayer player) {EffectHelpers.refreshPersistentEffect(player, effect, amplifier);}
        @Override
        public void remove(ServerPlayer player) {}
        @Override
        public boolean eachTick() {return true;}
    }

    private record Attribut(String name, String id, Holder<Attribute> attribute, double modifier, AttributeModifier.Operation operation) implements RandomEffect {
        @Override
        public void apply(ServerPlayer player) {player.getAttributes().addTransientAttributeModifiers(getMap());}
        @Override
        public void remove(ServerPlayer player) {player.getAttributes().removeAttributeModifiers(getMap());}
        @Override
        public boolean eachTick() {return false;}

        private Multimap<Holder<Attribute>, AttributeModifier> getMap() {
            Multimap<Holder<Attribute>, AttributeModifier> map = HashMultimap.create(1, 1);
            map.put(attribute, new AttributeModifier(Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, "random_effect_" + id), modifier, operation));
            return map;
        }
    }
}
