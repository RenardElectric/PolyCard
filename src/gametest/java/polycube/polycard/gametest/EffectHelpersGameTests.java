package polycube.polycard.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.monster.zombie.Zombie;
import polycube.polycard.utils.EffectHelpers;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EffectHelpersGameTests {
    @GameTest(maxTicks = 30)
    public void persistentEffectLifecycle(GameTestHelper helper) {
        EffectHelpers.clearPersistentEffectState();
        Zombie entity = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));

        managedDisplayFlagsAreStable(helper, entity);
        stableRefreshAndExpiry(helper, entity);
        overlappingAmplifiers(helper, entity);
        interleavedManagedAndExternalAmplifiers(helper, entity);
        preExistingEffects(helper, entity);
        strongerExternalEffects(helper, entity);
        weakerExternalEffect(helper, entity);
        hiddenPromotion(helper, entity);
        reinstallAfterRemoval(helper, entity);
        unrelatedInfiniteEffect(helper, entity);
        normalizedAmplifiers(helper, entity);
        persistenceFiltering(helper, entity);
        attributePersistence(helper, entity);
        absorptionPersistence(helper, entity);
        saveLoadPersistence(helper, entity);
        conversionPersistence(helper);
        expiredHiddenLayer(helper, entity);
        callbackEfficiency(helper);

        verifyRealTickLifecycle(helper, entity);
    }

    private static void managedDisplayFlagsAreStable(GameTestHelper helper, Zombie entity) {
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 1);
        assertChain(helper, entity, 1);
        assertManagedDisplayFlags(helper, entity);
        EffectHelpers.onEndServerTick();

        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 1);
        entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0));
        assertChain(helper, entity, 1, 0);
        assertManagedDisplayFlags(helper, entity);
        EffectHelpers.onEndServerTick();
        assertChain(helper, entity, 1, 0);
        assertManagedDisplayFlags(helper, entity);
        EffectHelpers.onEndServerTick();
        assertChain(helper, entity, 0);
        helper.assertTrue(!entity.getEffect(MobEffects.RESISTANCE).isAmbient(), "The external effect must not be ambient");
        helper.assertTrue(entity.getEffect(MobEffects.RESISTANCE).isVisible(), "The external effect must show particles");
        helper.assertTrue(entity.getEffect(MobEffects.RESISTANCE).showIcon(), "The external effect must show its icon");
        reset(entity);
    }

    private static void assertManagedDisplayFlags(GameTestHelper helper, Zombie entity) {
        var effect = entity.getEffect(MobEffects.RESISTANCE);
        helper.assertTrue(effect.isAmbient(), "The managed effect must be ambient");
        helper.assertTrue(!effect.isVisible(), "The managed effect must hide particles");
        helper.assertTrue(effect.showIcon(), "The managed effect must show its icon");
    }

    private static void stableRefreshAndExpiry(GameTestHelper helper, Zombie entity) {
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        assertChain(helper, entity, 0);
        EffectHelpers.onEndServerTick();

        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        assertChain(helper, entity, 0);
        EffectHelpers.onEndServerTick();

        EffectHelpers.onEndServerTick();
        helper.assertTrue(entity.getEffect(MobEffects.RESISTANCE) == null,
                "The effect must be removed on the first tick without a refresh");
    }

    private static void overlappingAmplifiers(GameTestHelper helper, Zombie entity) {
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 1);
        assertChain(helper, entity, 1, 0);
        EffectHelpers.onEndServerTick();

        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        EffectHelpers.onEndServerTick();
        assertChain(helper, entity, 0);

        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 1);
        EffectHelpers.onEndServerTick();
        assertChain(helper, entity, 1);

        EffectHelpers.onEndServerTick();
        helper.assertTrue(entity.getEffect(MobEffects.RESISTANCE) == null,
                "All managed amplifier layers must be removable together");
    }

    private static void interleavedManagedAndExternalAmplifiers(GameTestHelper helper, Zombie entity) {
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 2);
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        EffectHelpers.onEndServerTick();

        entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 1));
        assertChain(helper, entity, 2, 1, 0);

        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        EffectHelpers.onEndServerTick();
        assertChain(helper, entity, 1, 0);

        EffectHelpers.onEndServerTick();
        assertChain(helper, entity, 1);
        helper.assertTrue(entity.getEffect(MobEffects.RESISTANCE).getDuration() == 100,
                "Stopping either surrounding managed layer must retain the external middle layer");
        reset(entity);
    }

    private static void preExistingEffects(GameTestHelper helper, Zombie entity) {
        var sameAmplifier = new MobEffectInstance(MobEffects.RESISTANCE, 100, 0);
        entity.addEffect(sameAmplifier);
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        assertChain(helper, entity, 0, 0);
        EffectHelpers.onEndServerTick();
        EffectHelpers.onEndServerTick();
        helper.assertTrue(entity.getEffect(MobEffects.RESISTANCE) == sameAmplifier,
                "A same-amplifier finite effect must survive the managed lease");
        reset(entity);

        var stronger = new MobEffectInstance(MobEffects.RESISTANCE, 100, 2);
        entity.addEffect(stronger);
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        assertChain(helper, entity, 2, 0);
        EffectHelpers.onEndServerTick();
        EffectHelpers.onEndServerTick();
        helper.assertTrue(entity.getEffect(MobEffects.RESISTANCE) == stronger,
                "A stronger finite effect must remain active when a hidden managed lease ends");
        reset(entity);

        var weaker = new MobEffectInstance(MobEffects.RESISTANCE, 100, 0);
        entity.addEffect(weaker);
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 2);
        assertChain(helper, entity, 2, 0);
        EffectHelpers.onEndServerTick();
        EffectHelpers.onEndServerTick();
        helper.assertTrue(entity.getEffect(MobEffects.RESISTANCE) == weaker,
                "A weaker finite effect must be restored when the active managed lease ends");
        reset(entity);
    }

    private static void strongerExternalEffects(GameTestHelper helper, Zombie entity) {
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        EffectHelpers.onEndServerTick();

        var finiteExternal = new MobEffectInstance(MobEffects.RESISTANCE, 100, 2);
        entity.addEffect(finiteExternal);
        assertChain(helper, entity, 2, 0);
        EffectHelpers.onEndServerTick();
        assertChain(helper, entity, 2);
        helper.assertTrue(entity.getEffect(MobEffects.RESISTANCE).getDuration() == 100,
                "Removing the managed layer must retain the stronger finite effect");
        reset(entity);

        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        EffectHelpers.onEndServerTick();
        entity.addEffect(new MobEffectInstance(
                MobEffects.RESISTANCE, MobEffectInstance.INFINITE_DURATION, 2
        ));
        assertChain(helper, entity, 2, 0);
        EffectHelpers.onEndServerTick();
        assertChain(helper, entity, 2);
        reset(entity);
    }

    private static void weakerExternalEffect(GameTestHelper helper, Zombie entity) {
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 2);
        EffectHelpers.onEndServerTick();

        entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 0));
        assertChain(helper, entity, 2, 0);
        entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 0));
        assertChain(helper, entity, 2, 0);
        EffectHelpers.onEndServerTick();

        var restored = entity.getEffect(MobEffects.RESISTANCE);
        helper.assertTrue(restored != null && restored.getAmplifier() == 0 && restored.getDuration() == 100,
                "A repeated weaker external effect must merge without growing the hidden chain");
        reset(entity);
    }

    private static void hiddenPromotion(GameTestHelper helper, Zombie entity) {
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        EffectHelpers.onEndServerTick();
        entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1, 2));

        var active = entity.getEffect(MobEffects.RESISTANCE);
        helper.assertTrue(active != null && active.tickServer(helper.getLevel(), entity, () -> {}),
                "The stronger finite effect must promote its hidden managed layer");
        assertChain(helper, entity, 0);

        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        assertChain(helper, entity, 0);
        EffectHelpers.onEndServerTick();
        EffectHelpers.onEndServerTick();
        helper.assertTrue(entity.getEffect(MobEffects.RESISTANCE) == null,
                "A promoted managed layer must still be removable by its original lease");
    }

    private static void reinstallAfterRemoval(GameTestHelper helper, Zombie entity) {
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 2);
        entity.removeAllEffects();
        EffectHelpers.onEndServerTick();
        assertChain(helper, entity, 2, 0);

        EffectHelpers.onEndServerTick();
        helper.assertTrue(entity.getEffect(MobEffects.RESISTANCE) == null,
                "A refreshed effect removed mid-tick must be restored only for that lease tick");
    }

    private static void unrelatedInfiniteEffect(GameTestHelper helper, Zombie entity) {
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        EffectHelpers.onEndServerTick();

        entity.addEffect(new MobEffectInstance(
                MobEffects.RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0
        ));
        var externalLayer = entity.getEffect(MobEffects.RESISTANCE).hiddenEffect;
        EffectHelpers.onEndServerTick();
        helper.assertTrue(entity.getEffect(MobEffects.RESISTANCE) == externalLayer,
                "An identical external infinite effect must take over without replacing its active state");
        reset(entity);

        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        EffectHelpers.onEndServerTick();
        entity.removeAllEffects();

        var unrelated = new MobEffectInstance(
                MobEffects.RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0
        );
        entity.addEffect(unrelated);
        EffectHelpers.onEndServerTick();
        helper.assertTrue(entity.getEffect(MobEffects.RESISTANCE) == unrelated,
                "An unrelated property-identical infinite effect must never be adopted or removed");
        reset(entity);
    }

    private static void normalizedAmplifiers(GameTestHelper helper, Zombie entity) {
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 255);
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 256);
        assertChain(helper, entity, 255);
        EffectHelpers.onEndServerTick();
        EffectHelpers.onEndServerTick();
        reset(entity);

        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, -1);
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        assertChain(helper, entity, 0);
        EffectHelpers.onEndServerTick();
        EffectHelpers.onEndServerTick();
    }

    private static void persistenceFiltering(GameTestHelper helper, Zombie entity) {
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 2);
        EffectHelpers.onEndServerTick();
        entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0));

        var expired = new MobEffectInstance(MobEffects.RESISTANCE, 0, 1);
        expired.hiddenEffect = entity.getEffect(MobEffects.RESISTANCE).hiddenEffect;
        entity.getEffect(MobEffects.RESISTANCE).hiddenEffect = expired;

        var filtered = EffectHelpers.effectsForSave(List.of(entity.getEffect(MobEffects.RESISTANCE)));
        helper.assertTrue(filtered.size() == 1, "The external effect must remain saveable");
        helper.assertTrue(filtered.getFirst().getAmplifier() == 0 && filtered.getFirst().getDuration() == 60,
                "Transient managed layers must be excluded from saved effect chains");
        helper.assertTrue(filtered.getFirst().hiddenEffect == null,
                "Saving must not retain a managed hidden layer");

        EffectHelpers.onEndServerTick();
        reset(entity);
    }

    private static void attributePersistence(GameTestHelper helper, Zombie entity) {
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.SPEED, 2);
        EffectHelpers.onEndServerTick();

        var managedModifierValues = effectModifierValues(MobEffects.SPEED.value(), 2);
        var liveModifierValues = packedModifierValues(entity.getAttributes().pack());
        helper.assertTrue(liveModifierValues.entrySet().containsAll(managedModifierValues.entrySet()),
                "The active managed effect must apply its normal attribute modifiers");

        var managedOnlySave = EffectHelpers.attributesForSave(
                entity.getAttributes().pack(), entity.getActiveEffects()
        );
        helper.assertTrue(managedModifierValues.keySet().stream()
                        .noneMatch(packedModifierValues(managedOnlySave)::containsKey),
                "A managed-only effect must not leave a permanent modifier in saved attributes");

        entity.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0));
        var externalModifierValues = effectModifierValues(MobEffects.SPEED.value(), 0);
        var externalSaveValues = packedModifierValues(EffectHelpers.attributesForSave(
                entity.getAttributes().pack(), entity.getActiveEffects()
        ));
        helper.assertTrue(externalSaveValues.entrySet().containsAll(externalModifierValues.entrySet()),
                "Saved attributes must use the first surviving external effect amplifier");

        Zombie copiedEntity = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 1, 1));
        copiedEntity.getAttributes().assignPermanentModifiers(entity.getAttributes());
        EffectHelpers.reconcileCopiedEffectAttributes(copiedEntity, entity.getActiveEffects());
        var reconciledValues = packedModifierValues(copiedEntity.getAttributes().pack());
        helper.assertTrue(reconciledValues.entrySet().containsAll(externalModifierValues.entrySet()),
                "Keep-effects attribute copying must reconcile to the external amplifier before restoration");
        copiedEntity.discard();

        EffectHelpers.onEndServerTick();
        reset(entity);
    }

    private static void absorptionPersistence(GameTestHelper helper, Zombie entity) {
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.ABSORPTION, 2);
        EffectHelpers.onEndServerTick();
        helper.assertTrue(entity.getAbsorptionAmount() == 12.0F,
                "The managed absorption effect must apply normally while leased");
        helper.assertTrue(EffectHelpers.externalAbsorptionAmount(
                        entity, entity.getAbsorptionAmount()) == 0.0F,
                "Managed-only absorption must not leak into saved or converted state");

        entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60, 0));
        helper.assertTrue(EffectHelpers.externalAbsorptionAmount(
                        entity, entity.getAbsorptionAmount()) == 4.0F,
                "Copied absorption must be capped to the surviving external effect");

        EffectHelpers.onEndServerTick();
        reset(entity);
    }

    private static void saveLoadPersistence(GameTestHelper helper, Zombie entity) {
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 2);
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.SPEED, 2);
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.ABSORPTION, 2);
        EffectHelpers.onEndServerTick();

        entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0));
        entity.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0));
        entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60, 0));

        var restored = new Zombie(helper.getLevel());
        restored.restoreFrom(entity);
        assertChain(helper, restored, 0);
        helper.assertTrue(restored.getEffect(MobEffects.SPEED) != null
                        && restored.getEffect(MobEffects.SPEED).getAmplifier() == 0
                        && restored.getEffect(MobEffects.SPEED).hiddenEffect == null,
                "Entity save/load must retain only the external speed effect");
        helper.assertTrue(packedModifierValues(restored.getAttributes().pack()).entrySet()
                        .containsAll(effectModifierValues(MobEffects.SPEED.value(), 0).entrySet()),
                "Entity save/load must retain the external effect's attribute amplifier");
        helper.assertTrue(restored.getEffect(MobEffects.ABSORPTION) != null
                        && restored.getEffect(MobEffects.ABSORPTION).getAmplifier() == 0
                        && restored.getAbsorptionAmount() == 4.0F,
                "Entity save/load must cap absorption to the surviving external effect");
        restored.discard();

        EffectHelpers.onEndServerTick();
        reset(entity);
    }

    private static void conversionPersistence(GameTestHelper helper) {
        Zombie source = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(3, 1, 1));
        EffectHelpers.refreshPersistentEffect(source, MobEffects.RESISTANCE, 2);
        EffectHelpers.refreshPersistentEffect(source, MobEffects.SPEED, 2);
        EffectHelpers.refreshPersistentEffect(source, MobEffects.ABSORPTION, 2);
        EffectHelpers.onEndServerTick();

        source.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0));
        source.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0));
        source.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60, 0));

        Zombie converted = source.convertTo(
                EntityTypes.ZOMBIE, ConversionParams.single(source, false, false), _ -> {}
        );
        helper.assertTrue(converted != null, "The conversion test entity must be created");
        assertChain(helper, converted, 0);
        helper.assertTrue(converted.getEffect(MobEffects.SPEED) != null
                        && converted.getEffect(MobEffects.SPEED).getAmplifier() == 0,
                "Mob conversion must retain only the external attribute effect");
        helper.assertTrue(converted.getEffect(MobEffects.ABSORPTION) != null
                        && converted.getEffect(MobEffects.ABSORPTION).getAmplifier() == 0
                        && converted.getAbsorptionAmount() == 4.0F,
                "Mob conversion must not copy managed absorption state");

        EffectHelpers.onEndServerTick();
        converted.discard();
    }

    private static void expiredHiddenLayer(GameTestHelper helper, Zombie entity) {
        var external = new MobEffectInstance(MobEffects.RESISTANCE, 100, 2);
        var expired = new MobEffectInstance(MobEffects.RESISTANCE, 0, 1);
        var surviving = new MobEffectInstance(MobEffects.RESISTANCE, 50, 0);
        external.hiddenEffect = expired;
        expired.hiddenEffect = surviving;
        entity.addEffect(external);

        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 3);
        EffectHelpers.onEndServerTick();
        assertChain(helper, entity, 3, 2, 0);
        helper.assertTrue(entity.getEffect(MobEffects.RESISTANCE).hiddenEffect.hiddenEffect == surviving,
                "Expired hidden layers must be pruned while every lease is still refreshed");

        EffectHelpers.onEndServerTick();
        assertChain(helper, entity, 2, 0);
        helper.assertTrue(entity.getEffect(MobEffects.RESISTANCE).hiddenEffect == surviving,
                "Cleanup must skip an expired hidden layer without losing deeper effects");
        reset(entity);
    }

    private static void callbackEfficiency(GameTestHelper helper) {
        var entity = new CountingZombie(helper);

        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        EffectHelpers.onEndServerTick();
        entity.clearCounts();
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        EffectHelpers.onEndServerTick();
        assertCounts(helper, entity, 0, 0, 0,
                "A stable refresh must not trigger an add, update, or removal");

        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 1);
        EffectHelpers.onEndServerTick();
        entity.clearCounts();
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 1);
        EffectHelpers.onEndServerTick();
        assertCounts(helper, entity, 0, 0, 0,
                "Removing a lower hidden lease must not trigger an active-effect callback");

        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 1);
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        EffectHelpers.onEndServerTick();
        entity.clearCounts();
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        EffectHelpers.onEndServerTick();
        assertCounts(helper, entity, 0, 1, 0,
                "Replacing an expired stronger lease with a lower lease must use one update");

        entity.clearCounts();
        EffectHelpers.onEndServerTick();
        assertCounts(helper, entity, 0, 0, 1,
                "Ending the final lease must use one removal");

        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        EffectHelpers.onEndServerTick();
        entity.addEffect(new MobEffectInstance(
                MobEffects.RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, true, false, true
        ));
        entity.clearCounts();
        EffectHelpers.onEndServerTick();
        assertCounts(helper, entity, 0, 0, 0,
                "An identical external infinite handoff must be callback- and packet-free");
        reset(entity);

        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 2);
        EffectHelpers.onEndServerTick();
        entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0));
        entity.clearCounts();
        EffectHelpers.onEndServerTick();
        assertCounts(helper, entity, 0, 1, 0,
                "Restoring a finite external effect must use one update and no removal");
        reset(entity);

        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 2);
        EffectHelpers.onEndServerTick();
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 0);
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 2);
        entity.removeAllEffects();
        entity.clearCounts();
        EffectHelpers.onEndServerTick();
        assertCounts(helper, entity, 1, 0, 0,
                "Reinstalling several refreshed leases after milk must add the strongest chain once");

        EffectHelpers.onEndServerTick();
        entity.discard();
    }

    private static void verifyRealTickLifecycle(GameTestHelper helper, Zombie entity) {
        reset(entity);
        EffectHelpers.refreshPersistentEffect(entity, MobEffects.RESISTANCE, 2);
        entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 10, 0));
        int initialExternalDuration = entity.getEffect(MobEffects.RESISTANCE).hiddenEffect.getDuration();

        helper.runAfterDelay(3, () -> {
            var restored = entity.getEffect(MobEffects.RESISTANCE);
            helper.assertTrue(restored != null && restored.getAmplifier() == 0,
                    "The registered end-of-tick sweep must expire an unrefreshed lease");
            helper.assertTrue(restored.getDuration() < initialExternalDuration,
                    "A finite external effect must keep counting down while hidden");
            EffectHelpers.clearPersistentEffectState();
            entity.removeAllEffects();
            helper.succeed();
        });
    }

    private static void assertCounts(GameTestHelper helper, CountingZombie entity,
                                     int added, int updated, int removed, String message) {
        helper.assertTrue(entity.added == added && entity.updated == updated && entity.removed == removed,
                message + " (actual: " + entity.added + " add, " + entity.updated
                        + " update, " + entity.removed + " remove)");
    }

    private static void assertChain(GameTestHelper helper, Zombie entity, int... amplifiers) {
        MobEffectInstance effect = entity.getEffect(MobEffects.RESISTANCE);
        for (int amplifier : amplifiers) {
            helper.assertTrue(effect != null, "Effect chain ended before amplifier " + amplifier);
            helper.assertTrue(effect.getAmplifier() == amplifier, "Expected amplifier " + amplifier + " but found " + effect.getAmplifier());
            effect = effect.hiddenEffect;
        }
        helper.assertTrue(effect == null, "Effect chain contains an unexpected extra layer");
    }

    private static void reset(Zombie entity) {
        entity.removeAllEffects();
        EffectHelpers.onEndServerTick();
    }

    private static Map<Identifier, Double> effectModifierValues(
            MobEffect effect, int amplifier) {
        Map<Identifier, Double> values = new HashMap<>();
        effect.createModifiers(amplifier, (_, modifier) -> values.put(modifier.id(), modifier.amount()));
        return values;
    }

    private static Map<Identifier, Double> packedModifierValues(
            List<AttributeInstance.Packed> attributes) {
        Map<Identifier, Double> values = new HashMap<>();
        for (var attribute : attributes) {
            for (var modifier : attribute.modifiers()) {
                values.put(modifier.id(), modifier.amount());
            }
        }
        return values;
    }

    private static final class CountingZombie extends Zombie {
        private int added;
        private int updated;
        private int removed;

        private CountingZombie(GameTestHelper helper) {
            super(helper.getLevel());
        }

        @Override
        protected void onEffectAdded(MobEffectInstance effect, Entity source) {
            super.onEffectAdded(effect, source);
            added++;
        }

        @Override
        protected void onEffectUpdated(MobEffectInstance effect, boolean refreshAttributes, Entity source) {
            super.onEffectUpdated(effect, refreshAttributes, source);
            updated++;
        }

        @Override
        protected void onEffectsRemoved(Collection<MobEffectInstance> effects) {
            super.onEffectsRemoved(effects);
            removed++;
        }

        private void clearCounts() {
            added = 0;
            updated = 0;
            removed = 0;
        }
    }
}
