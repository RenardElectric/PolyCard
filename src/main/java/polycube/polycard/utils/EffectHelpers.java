package polycube.polycard.utils;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jspecify.annotations.Nullable;

import java.util.*;

/// Maintains packet-efficient, tick-refreshed status-effect layers.
///
/// Each call leases one effect/amplifier pair through the end of the current server tick. A
/// lease is removed on the first tick in which it is not refreshed. Managed layers use infinite
/// duration and live in the vanilla hidden-effect chain, so finite effects continue counting down
/// normally while hidden.
///
/// Written by GPT-5.6 Sol Ultra (It was too complicated for me sadly)
public final class EffectHelpers {
    private static final Map<LivingEntity, Map<Holder<MobEffect>, EffectState>> ACTIVE_EFFECTS = new WeakHashMap<>();

    private EffectHelpers() {}

    /// Keeps an effect active until the first server tick in which this method is not called.
    /// Multiple callers requesting the same effect and amplifier share one aggregate lease.
    public static void refreshPersistentEffect(LivingEntity entity, Holder<MobEffect> effect, int amplifier) {
        int normalizedAmplifier = Mth.clamp(amplifier, MobEffectInstance.MIN_AMPLIFIER, MobEffectInstance.MAX_AMPLIFIER);
        var entityEffects = ACTIVE_EFFECTS.computeIfAbsent(entity, _ -> new HashMap<>());
        var effectState = entityEffects.computeIfAbsent(effect, _ -> new EffectState());
        var lease = effectState.byAmplifier.get(normalizedAmplifier);

        if (lease == null) {
            lease = new Lease(normalizedAmplifier);
            if (!installPersistentEffect(entity, effect, lease)) {
                removeEmptyState(entity, effect, entityEffects, effectState);
                return;
            }
            effectState.add(lease);
        } else if (findOwnedInstance(entity.getEffect(effect), lease) == null && !installPersistentEffect(entity, effect, lease)) {
            return;
        }

        lease.refreshed = true;
    }

    /// Expires leases not refreshed during this tick. This must run once after all refresh calls.
    public static void onEndServerTick() {
        for (var entityIterator = ACTIVE_EFFECTS.entrySet().iterator(); entityIterator.hasNext(); ) {
            var entityEntry = entityIterator.next();
            var entity = entityEntry.getKey();
            var entityEffects = entityEntry.getValue();

            entityEffects.entrySet().removeIf(effectEntry -> sweepEffect(entity, effectEntry.getKey(), effectEntry.getValue()));

            if (entityEffects.isEmpty()) {
                entityIterator.remove();
            }
        }
    }

    /// Clears server-scoped lease bookkeeping when a server shuts down.
    public static void clearPersistentEffectState() {
        for (var entityEffects : ACTIVE_EFFECTS.values()) {
            for (var state : entityEffects.values()) {
                for (var lease : state.byAmplifier.values()) {
                    lease.active = false;
                }
            }
        }
        ACTIVE_EFFECTS.clear();
    }

    /// Preserves an external same-or-weaker effect that vanilla would otherwise discard behind an
    /// infinite managed layer. Returns whether the incoming effect was fully handled without
    /// mutating the active managed layer.
    public static boolean beforeEffectUpdate(MobEffectInstance current, MobEffectInstance incoming) {
        var owner = validOwner(current);
        if (owner == null || validOwner(incoming) != null) {
            return false;
        }

        if (incoming.getAmplifier() <= current.getAmplifier()) {
            var externalCopy = copyUnowned(incoming, null);
            if (hasRemainingDuration(externalCopy)) {
                if (current.hiddenEffect == null) {
                    current.hiddenEffect = externalCopy;
                } else {
                    current.hiddenEffect.update(externalCopy);
                }
            }
            return true;
        } else if (incoming.isInfiniteDuration()) {
            // Vanilla does not make its usual hidden copy when both effects are infinite.
            var managedCopy = copyUnowned(current, current.hiddenEffect);
            setOwner(managedCopy, owner);
            current.hiddenEffect = managedCopy;
        }
        return false;
    }

    /// Transfers ownership after vanilla mutates the current instance during an update.
    public static void afterEffectUpdate(MobEffectInstance current, int previousAmplifier,
                                         @Nullable Object previousOwner, @Nullable Object incomingOwner,
                                         @Nullable MobEffectInstance hiddenBeforeUpdate) {
        if (current.getAmplifier() <= previousAmplifier) {
            return;
        }

        if (previousOwner instanceof Lease && current.hiddenEffect != hiddenBeforeUpdate) {
            setOwner(current.hiddenEffect, previousOwner);
        }
        setOwner(current, incomingOwner);
    }

    /// Transfers ownership when vanilla promotes a hidden instance into the active object.
    public static void afterHiddenEffectPromotion(MobEffectInstance current,
                                                  @Nullable MobEffectInstance promotedEffect,
                                                  @Nullable Object promotedOwner,
                                                  boolean promoted) {
        if (promoted) {
            setOwner(current, promotedOwner);
            setOwner(promotedEffect, null);
        }
    }

    /// Returns the valid managed owner token attached to an instance, if any.
    public static @Nullable Object persistentEffectOwner(@Nullable MobEffectInstance instance) {
        return validOwner(instance);
    }

    /// Produces the active-effect list used for entity saving, excluding transient managed layers.
    /// External nodes are cloned only when a chain actually contains a managed layer.
    public static List<MobEffectInstance> effectsForSave(List<MobEffectInstance> effects) {
        return (List<MobEffectInstance>) effectsForConversion(effects);
    }

    /// Returns saved attributes as they would be without transient managed layers. Effect modifiers
    /// are permanent modifiers in vanilla and therefore need to be reconciled independently of
    /// the saved effect list.
    public static List<AttributeInstance.Packed> attributesForSave(List<AttributeInstance.Packed> attributes, Collection<MobEffectInstance> effects) {
        Map<Holder<Attribute>, AttributeChanges> changes = new HashMap<>();

        for (var effect : effects) {
            if (!containsManagedLayer(effect)) {
                continue;
            }

            var externalEffect = firstExternalLayer(effect);
            effect.getEffect().value().createModifiers(0, (attribute, modifier) ->
                    changes.computeIfAbsent(attribute, _ -> new AttributeChanges()).remove(modifier.id())
            );
            if (externalEffect != null) {
                effect.getEffect().value().createModifiers(externalEffect.getAmplifier(), (attribute, modifier) ->
                        changes.computeIfAbsent(attribute, _ -> new AttributeChanges()).replace(modifier)
                );
            }
        }

        if (changes.isEmpty()) {
            return attributes;
        }

        List<AttributeInstance.Packed> filtered = new ArrayList<>(attributes.size());
        for (var attribute : attributes) {
            var attributeChanges = changes.get(attribute.attribute());
            if (attributeChanges == null) {
                filtered.add(attribute);
                continue;
            }

            List<AttributeModifier> modifiers = new ArrayList<>(attribute.modifiers().size() + attributeChanges.replacements.size());
            for (var modifier : attribute.modifiers()) {
                if (!attributeChanges.effectModifierIds.contains(modifier.id())) {
                    modifiers.add(modifier);
                }
            }
            modifiers.addAll(attributeChanges.replacements.values());
            filtered.add(new AttributeInstance.Packed(attribute.attribute(), attribute.baseValue(), List.copyOf(modifiers)));
        }
        return List.copyOf(filtered);
    }

    /// Returns absorption state capped to the effect layers that are allowed to survive copying.
    public static float externalAbsorptionAmount(LivingEntity entity, float absorptionAmount) {
        var absorptionEffect = entity.getEffect(MobEffects.ABSORPTION);
        if (validOwner(absorptionEffect) == null) {
            return absorptionAmount;
        }

        var externalAttributes = attributesForSave(
                entity.getAttributes().pack(), entity.getActiveEffects()
        );
        for (var attribute : externalAttributes) {
            if (attribute.attribute().equals(Attributes.MAX_ABSORPTION)) {
                return Math.min(absorptionAmount, (float) calculateAttributeValue(attribute));
            }
        }
        return 0.0F;
    }

    /// Reconciles effect modifiers copied by keep-effects respawn before health is restored.
    public static void reconcileCopiedEffectAttributes(LivingEntity newEntity, Collection<MobEffectInstance> effects) {
        for (var effect : effects) {
            if (containsManagedLayer(effect)) {
                var mobEffect = effect.getEffect().value();
                mobEffect.removeAttributeModifiers(newEntity.getAttributes());
                var externalEffect = firstExternalLayer(effect);
                if (externalEffect != null) {
                    mobEffect.addAttributeModifiers(newEntity.getAttributes(), externalEffect.getAmplifier());
                }
            }
        }
    }

    /// Returns an external-only view for vanilla's keep-effects player copy.
    public static Collection<MobEffectInstance> effectsForRespawn(Collection<MobEffectInstance> effects) {
        return effectsForConversion(effects);
    }

    /// Returns an external-only view for mob conversion, which does not copy attributes first.
    public static Collection<MobEffectInstance> effectsForConversion(Collection<MobEffectInstance> effects) {
        for (var effect : effects) {
            if (containsManagedLayer(effect)) {
                return copyExternalEffects(effects);
            }
        }
        return effects;
    }

    private static List<MobEffectInstance> copyExternalEffects(Collection<MobEffectInstance> effects) {
        List<MobEffectInstance> filtered = new ArrayList<>(effects.size());
        for (var effect : effects) {
            var externalEffect = copyWithoutManagedLayers(effect);
            if (externalEffect != null) {
                filtered.add(externalEffect);
            }
        }
        return List.copyOf(filtered);
    }

    private static boolean sweepEffect(LivingEntity entity, Holder<MobEffect> effect, EffectState state) {
        resolveOwnedInstances(entity.getEffect(effect), state);

        Set<Lease> expiredOwners = null;
        for (var lease : state.byAmplifier.values()) {
            if (!lease.refreshed) {
                if (expiredOwners == null) {
                    expiredOwners = Collections.newSetFromMap(new IdentityHashMap<>());
                }
                expiredOwners.add(lease);
            }
        }

        var currentEffect = entity.getEffect(effect);
        boolean silentExternalHandoff = expiredOwners != null
                && ownerOf(currentEffect) instanceof Lease owner
                && expiredOwners.contains(owner);
        var remainingEffect = removeOwnedAndExpiredLayers(currentEffect, expiredOwners == null ? List.of() : expiredOwners);
        replaceActiveEffect(entity, effect, currentEffect, remainingEffect, silentExternalHandoff);

        if (expiredOwners != null) {
            for (var iterator = state.byAmplifier.values().iterator(); iterator.hasNext(); ) {
                var lease = iterator.next();
                if (expiredOwners.contains(lease)) {
                    lease.active = false;
                    iterator.remove();
                }
            }
        }

        if (state.byAmplifier.isEmpty()) {
            return true;
        }

        // A removal such as milk can happen after the tick callback refreshed a lease.
        List<Lease> missingLeases = null;
        for (var lease : state.byAmplifier.values()) {
            if (lease.refreshed && lease.instance == null) {
                if (missingLeases == null) {
                    missingLeases = new ArrayList<>();
                }
                missingLeases.add(lease);
            }
            lease.refreshed = false;
        }
        if (missingLeases != null) {
            missingLeases.sort((left, right) -> Integer.compare(right.amplifier, left.amplifier));
            for (var lease : missingLeases) {
                installPersistentEffect(entity, effect, lease);
            }
        }
        return false;
    }

    private static void resolveOwnedInstances(@Nullable MobEffectInstance root, EffectState state) {
        for (var lease : state.byAmplifier.values()) {
            lease.instance = null;
        }

        for (var instance = root; instance != null; instance = instance.hiddenEffect) {
            var rawOwner = ownerOf(instance);
            if (!(rawOwner instanceof Lease owner)) {
                continue;
            }

            var lease = state.byAmplifier.get(owner.amplifier);
            if (lease == owner && owner.matches(instance) && owner.instance == null) {
                owner.instance = instance;
            } else {
                setOwner(instance, null);
            }
        }
    }

    private static @Nullable MobEffectInstance findOwnedInstance(@Nullable MobEffectInstance root, Lease lease) {
        MobEffectInstance ownedInstance = null;
        for (var instance = root; instance != null; instance = instance.hiddenEffect) {
            if (ownerOf(instance) != lease) {
                continue;
            }

            if (ownedInstance == null && lease.matches(instance)) {
                ownedInstance = instance;
            } else {
                setOwner(instance, null);
            }
        }
        lease.instance = ownedInstance;
        return ownedInstance;
    }

    private static boolean installPersistentEffect(LivingEntity entity, Holder<MobEffect> effect, Lease lease) {
        var persistentEffect = new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, lease.amplifier, true, false, true);
        if (!entity.canBeAffected(persistentEffect)) {
            return false;
        }

        setOwner(persistentEffect, lease);
        var currentEffect = entity.getEffect(effect);
        var resultingEffect = insertByAmplifier(currentEffect, persistentEffect);

        if (currentEffect == null) {
            if (!entity.addEffect(resultingEffect)) {
                setOwner(persistentEffect, null);
                return false;
            }
        } else {
            if (resultingEffect != currentEffect) {
                entity.forceAddEffect(resultingEffect, null);
            }
            persistentEffect.onEffectStarted(entity);
        }

        lease.instance = persistentEffect;
        return true;
    }

    private static MobEffectInstance insertByAmplifier(@Nullable MobEffectInstance currentEffect, MobEffectInstance insertedEffect) {
        if (currentEffect == null || currentEffect.getAmplifier() < insertedEffect.getAmplifier()) {
            insertedEffect.hiddenEffect = currentEffect;
            return insertedEffect;
        }

        currentEffect.hiddenEffect = insertByAmplifier(currentEffect.hiddenEffect, insertedEffect);
        return currentEffect;
    }

    private static @Nullable MobEffectInstance removeOwnedAndExpiredLayers(@Nullable MobEffectInstance root, Collection<Lease> expiredOwners) {
        while (root != null && shouldRemove(root, expiredOwners)) {
            var removed = root;
            root = removed.hiddenEffect;
            removed.hiddenEffect = null;
            setOwner(removed, null);
        }

        for (var parent = root; parent != null; ) {
            var child = parent.hiddenEffect;
            while (child != null && shouldRemove(child, expiredOwners)) {
                var removed = child;
                child = removed.hiddenEffect;
                removed.hiddenEffect = null;
                setOwner(removed, null);
            }
            parent.hiddenEffect = child;
            parent = child;
        }
        return root;
    }

    private static boolean shouldRemove(MobEffectInstance effect, Collection<Lease> expiredOwners) {
        var owner = ownerOf(effect);
        return !hasRemainingDuration(effect) || owner instanceof Lease && expiredOwners.contains(owner);
    }

    private static void replaceActiveEffect(LivingEntity entity, Holder<MobEffect> effect,
                                            @Nullable MobEffectInstance currentEffect,
                                            @Nullable MobEffectInstance replacement,
                                            boolean allowSilentExternalHandoff) {
        if (currentEffect == replacement) {
            return;
        }
        if (replacement == null) {
            entity.removeEffect(effect);
        } else if (currentEffect == null) {
            entity.addEffect(replacement);
        } else if (allowSilentExternalHandoff
                && ownerOf(replacement) == null
                && currentEffect.equals(replacement)) {
            replacement.copyBlendState(currentEffect);
            entity.getActiveEffectsMap().put(effect, replacement);
        } else {
            entity.forceAddEffect(replacement, null);
        }
    }

    private static boolean hasRemainingDuration(MobEffectInstance effect) {
        return effect.isInfiniteDuration() || effect.getDuration() > 0;
    }

    private static @Nullable Object validOwner(@Nullable MobEffectInstance instance) {
        var owner = ownerOf(instance);
        return owner instanceof Lease lease && lease.matches(instance) ? owner : null;
    }

    private static @Nullable Object ownerOf(@Nullable MobEffectInstance instance) {
        return instance == null ? null : ((MobEffectInstanceDuck) instance).polycard$getPersistentEffectOwner();
    }

    private static void setOwner(@Nullable MobEffectInstance instance, @Nullable Object owner) {
        if (instance != null) {
            ((MobEffectInstanceDuck) instance).polycard$setPersistentEffectOwner(owner);
        }
    }

    private static MobEffectInstance copyUnowned(MobEffectInstance effect, @Nullable MobEffectInstance hiddenEffect) {
        var newEffect = new MobEffectInstance(effect);
        newEffect.hiddenEffect = hiddenEffect;
        return newEffect;
    }

    private static boolean containsManagedLayer(MobEffectInstance effect) {
        for (var instance = effect; instance != null; instance = instance.hiddenEffect) {
            if (validOwner(instance) != null) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable MobEffectInstance copyWithoutManagedLayers(@Nullable MobEffectInstance effect) {
        if (effect == null) {
            return null;
        }
        if (validOwner(effect) != null || !hasRemainingDuration(effect)) {
            return copyWithoutManagedLayers(effect.hiddenEffect);
        }
        return copyUnowned(effect, copyWithoutManagedLayers(effect.hiddenEffect));
    }

    private static @Nullable MobEffectInstance firstExternalLayer(@Nullable MobEffectInstance effect) {
        while (effect != null && (validOwner(effect) != null || !hasRemainingDuration(effect))) {
            effect = effect.hiddenEffect;
        }
        return effect;
    }

    private static double calculateAttributeValue(AttributeInstance.Packed attribute) {
        double baseValue = attribute.baseValue();
        for (var modifier : attribute.modifiers()) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                baseValue += modifier.amount();
            }
        }

        double value = baseValue;
        for (var modifier : attribute.modifiers()) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                value += baseValue * modifier.amount();
            }
        }
        for (var modifier : attribute.modifiers()) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                value *= 1.0 + modifier.amount();
            }
        }
        return attribute.attribute().value().sanitizeValue(value);
    }

    private static void removeEmptyState(LivingEntity entity, Holder<MobEffect> effect,
                                         Map<Holder<MobEffect>, EffectState> entityEffects,
                                         EffectState effectState) {
        if (effectState.byAmplifier.isEmpty()) {
            entityEffects.remove(effect);
            if (entityEffects.isEmpty()) {
                ACTIVE_EFFECTS.remove(entity);
            }
        }
    }

    private static final class EffectState {
        private final Map<Integer, Lease> byAmplifier = new HashMap<>();

        private void add(Lease lease) {
            byAmplifier.put(lease.amplifier, lease);
        }
    }

    private static final class Lease {
        private final int amplifier;
        private boolean active = true;
        private boolean refreshed;
        private @Nullable MobEffectInstance instance;

        private Lease(int amplifier) {
            this.amplifier = amplifier;
        }

        private boolean matches(MobEffectInstance effect) {
            return active && effect.isInfiniteDuration() && effect.getAmplifier() == amplifier;
        }
    }

    private static final class AttributeChanges {
        private final Set<Identifier> effectModifierIds = new HashSet<>();
        private final Map<Identifier, AttributeModifier> replacements = new HashMap<>();

        private void remove(Identifier id) {
            effectModifierIds.add(id);
        }

        private void replace(AttributeModifier modifier) {
            effectModifierIds.add(modifier.id());
            replacements.put(modifier.id(), modifier);
        }
    }
}
