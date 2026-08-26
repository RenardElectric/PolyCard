package polycube.polycard.cardEffects.passive;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import polycube.polycard.card.Card;
import polycube.polycard.card.RarityLevel;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.CardEventCallback;
import polycube.polycard.events.callBacks.EntityAfterHurtEventCallback;
import polycube.polycard.events.callBacks.PlayerTickEventCallback;
import polycube.polycard.utils.EffectHelpers;

public final class GoatEffects
        extends CardEffects
        implements PlayerTickEventCallback, EntityAfterHurtEventCallback,
        ServerLivingEntityEvents.AfterDeath, CardEventCallback.CardUnequipEvent
{
    public static final float KNOCKBACK_REDUCTION = 1.3F;
    public static final int JUMP_BOOST_EFFECT_AMPLIFIER = 0;
    public static final double SPRINT_HIT_EXTRA_KNOCKBACK = 0.75;
    public static final int LEGENDARY_SPRINT_DURATION = 20 * 10;
    public static final double LEGENDARY_UPWARD_LAUNCH_VELOCITY = 0.5;

    private final PlayerState<Integer> sprintTicks = new PlayerState<>();

    public GoatEffects() {
        addAttribute(
                RarityLevel.UNCOMMON, Attributes.KNOCKBACK_RESISTANCE,
                "goat_knockback_resistance", AttributeModifier.Operation.ADD_VALUE,
                KNOCKBACK_REDUCTION
        );
    }

    @Override
    public void onPlayerTick(MinecraftServer server, ServerPlayer player) {
        var rarity = equippedRarityLevel(player).orElse(null);
        if (rarity == null) {
            sprintTicks.remove(player);
            return;
        }

        if (rarity.isAtLeast(RarityLevel.RARE) && player.level().getBiome(player.blockPosition()).is(BiomeTags.IS_MOUNTAIN)) {
            EffectHelpers.refreshPersistentEffect(player, MobEffects.JUMP_BOOST, JUMP_BOOST_EFFECT_AMPLIFIER);
        }

        if (rarity.isAtLeast(RarityLevel.LEGENDARY) && player.isAlive() && player.isSprinting()) {
            int previousTicks = sprintTicks.getOrDefault(player, 0);
            int currentTicks = Math.min(previousTicks + 1, LEGENDARY_SPRINT_DURATION);
            sprintTicks.put(player, currentTicks);
        } else {
            sprintTicks.remove(player);
        }
    }

    @Override
    public void afterEntityHurt(LivingEntity entity, ServerLevel level, DamageSource source, float damageDealt) {
        if (source.getEntity() instanceof ServerPlayer player) {
            var rarity = equippedRarityLevel(player).orElse(null);
            if (rarity == null || !rarity.isAtLeast(RarityLevel.EPIC)) {
                return;
            }
            var verticalVelocity = 0.0;
            if (rarity.isAtLeast(RarityLevel.LEGENDARY) && sprintTicks.getOrDefault(player, 0) >= 0) {
                verticalVelocity = LEGENDARY_UPWARD_LAUNCH_VELOCITY;
            }

            var horizontalPower = SPRINT_HIT_EXTRA_KNOCKBACK * (1.0 - entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            var verticalPower = verticalVelocity * (1.0 - entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            if (!(horizontalPower <= 0.0) || !(verticalPower <= 0.0)) {
                double xd = Mth.sin(player.getYRot() * Math.PI / 180.0);
                double zd = -Mth.cos(player.getYRot() * Math.PI / 180.0);
                entity.needsSync = true;
                Vec3 deltaVector = new Vec3(xd, 0.0, zd).normalize().scale(horizontalPower);
                Vec3 deltaMovement = entity.getDeltaMovement().subtract(deltaVector).add(0.0, verticalPower, 0.0);
                entity.setDeltaMovement(deltaMovement);
            }
        }
    }

    @Override
    public void onCardUnequip(ServerPlayer player, Card card) {
        if (card.cardType() == cardType()) {
            sprintTicks.remove(player);
        }
    }

    @Override
    public void afterDeath(LivingEntity entity, DamageSource source) {
        if (entity instanceof ServerPlayer player) {
            sprintTicks.remove(player);
        }
    }
}
