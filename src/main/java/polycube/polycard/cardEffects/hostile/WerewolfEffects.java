package polycube.polycard.cardEffects.hostile;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import polycube.polycard.PolyCard;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.PlayerSecondEventCallback;

import java.util.Objects;

public final class WerewolfEffects extends CardEffects implements PlayerSecondEventCallback {

    private static final float MIN_AMOUNT = -0.75f;
    private static final float MAX_AMOUNT = 0.75f;
    private static final float AMOUNT_RANGE = MAX_AMOUNT - MIN_AMOUNT;

    private final WerewolfType werewolfType;

    public WerewolfEffects(WerewolfType werewolfType) {
        this.werewolfType = werewolfType;
        addAttribute(RarityLevel.COMMON, werewolfType.attribute, werewolfType.id, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, player -> getEffectAmount(player, RarityLevel.COMMON));
        addAttribute(RarityLevel.UNCOMMON, werewolfType.attribute, werewolfType.id, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, player -> getEffectAmount(player, RarityLevel.UNCOMMON));
        addAttribute(RarityLevel.RARE, werewolfType.attribute, werewolfType.id, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, player -> getEffectAmount(player, RarityLevel.RARE));
        addAttribute(RarityLevel.EPIC, werewolfType.attribute, werewolfType.id, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, player -> getEffectAmount(player, RarityLevel.EPIC));
        addAttribute(RarityLevel.LEGENDARY, werewolfType.attribute, werewolfType.id, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, player -> getEffectAmount(player, RarityLevel.LEGENDARY));
    }

    @Override
    public void onPlayerSecond(MinecraftServer server, ServerPlayer player) {
        if (hasCardOrRarer(player, RarityLevel.COMMON)) {
            var attribute = player.getAttribute(werewolfType.attribute);
            if (attribute == null) return;
            var modifier = attribute.getModifier(Identifier.fromNamespaceAndPath(PolyCard.MOD_ID, werewolfType.id));
            if (modifier == null) return;
            var current = modifier.amount();
            if (getEffectAmount(player, Objects.requireNonNull(equippedRarityLevel(player))) != current) {
                loadAttributes(player);
            }
        }
    }

    public double getEffectAmount(ServerPlayer player, RarityLevel rarity) {
        double phaseIndex = player.level().environmentAttributes().getDimensionValue(EnvironmentAttributes.MOON_PHASE).index();
        if (phaseIndex > 4) phaseIndex = 8 - phaseIndex;
        phaseIndex /= 4.0;
        double amount = MAX_AMOUNT - AMOUNT_RANGE * phaseIndex;
        return amount * (rarity.rank() + 1) / 5.0;
    }

    public enum WerewolfType {
        ALPHA("alpha_werewolf", Attributes.ATTACK_DAMAGE),
        ELDER("elder_werewolf", Attributes.MAX_HEALTH);

        public final String id;
        public final Holder<Attribute> attribute;
        WerewolfType(String id, Holder<Attribute> attribute) {
            this.id = id;
            this.attribute = attribute;
        }
    }
}
