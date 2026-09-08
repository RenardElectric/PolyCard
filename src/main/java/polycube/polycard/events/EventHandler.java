package polycube.polycard.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import polycube.polycard.events.callBacks.*;

import java.util.Set;

public class EventHandler {
    public record EventRegistration<T>(Class<T> type, Event<T> event) {
    }

    public static <T> EventRegistration<T> of(Class<T> type, Event<T> event) {
        return new EventRegistration<>(type, event);
    }

    public static final Set<EventRegistration<?>> REGISTRATIONS = Set.of(
            of(BreedEventCallback.class, BreedEventCallback.EVENT),
            of(EntitySummonedEventCallback.class, EntitySummonedEventCallback.EVENT),
            of(TameEventCallback.class, TameEventCallback.EVENT),

            of(EntityHurtEventCallback.class, EntityHurtEventCallback.EVENT),
            of(EntityAfterHurtEventCallback.class, EntityAfterHurtEventCallback.EVENT),
            of(ExplosionKnockbackEventCallback.class, ExplosionKnockbackEventCallback.EVENT),
            of(PlayerKillEventCallback.class, PlayerKillEventCallback.EVENT),
            of(ProjectileOnHitEventCallback.class, ProjectileOnHitEventCallback.EVENT),
            of(ServerLivingEntityEvents.AllowDeath.class, ServerLivingEntityEvents.ALLOW_DEATH),
            of(ServerLivingEntityEvents.AfterDeath.class, ServerLivingEntityEvents.AFTER_DEATH),
            of(ServerLivingEntityEvents.AfterDamage.class, ServerLivingEntityEvents.AFTER_DAMAGE),

            of(IsTargetedEventCallback.class, IsTargetedEventCallback.EVENT),

            of(ItemConsumedEventCallback.class, ItemConsumedEventCallback.EVENT),
            of(ItemDurabilityChangeEventCallback.class, ItemDurabilityChangeEventCallback.EVENT),
            of(FallFlyingGliderWearEventCallback.class, FallFlyingGliderWearEventCallback.EVENT),
            of(ItemUseEventCallback.class, ItemUseEventCallback.EVENT),
            of(BlockEvents.UseItemOnCallback.class, BlockEvents.USE_ITEM_ON),
            of(PlayerBlockBreakEvents.After.class, PlayerBlockBreakEvents.AFTER),

            of(ServerPlayerEvents.Join.class, ServerPlayerEvents.JOIN),
            of(ServerPlayerEvents.AfterRespawn.class, ServerPlayerEvents.AFTER_RESPAWN),
            of(ServerPlayerEvents.Leave.class, ServerPlayerEvents.LEAVE),

            of(CardEventCallback.CardEquipEvent.class, CardEventCallback.EQUIPPED),
            of(CardEventCallback.CardUnequipEvent.class, CardEventCallback.UNEQUIPPED),
            of(ServerEntityEvents.EquipmentChange.class, ServerEntityEvents.EQUIPMENT_CHANGE),

            of(ServerTickEvents.EndLevelTick.class, ServerTickEvents.END_LEVEL_TICK),
            of(ServerTickEvents.EndTick.class, ServerTickEvents.END_SERVER_TICK),
            of(PlayerTickEventCallback.class, PlayerTickEventCallback.EVENT),
            of(PlayerSecondEventCallback.class, PlayerSecondEventCallback.EVENT),

            of(EquippedRarityLevelOverrideCallback.class, EquippedRarityLevelOverrideCallback.EVENT),
            of(CardProbabilityOverrideCallback.class, CardProbabilityOverrideCallback.EVENT),

            of(GetBedRuleEventCallback.class, GetBedRuleEventCallback.EVENT),
            of(KeepInventoryEventCallback.class, KeepInventoryEventCallback.EVENT),
            of(AllowPhantomSpawnEventCallback.class, AllowPhantomSpawnEventCallback.EVENT),

            of(LootTableEvents.Modify.class, LootTableEvents.MODIFY)
    );

    private boolean eventsRegistered;

    protected EventHandler() {}

    /// Registers every callback interface implemented by this fully constructed handler.
    public final void registerCallbacks() {
        if (eventsRegistered) throw new IllegalStateException(getClass().getName() + " registered its events more than once");
        validateCallbacks();
        REGISTRATIONS.forEach(this::handle);
        eventsRegistered = true;
    }

    /// Fails before registering anything if a handler implements a callback that was omitted from REGISTRATIONS.
    protected final void validateCallbacks() {
        for (Class<?> handlerType = getClass(); handlerType != EventHandler.class; handlerType = handlerType.getSuperclass()) {
            for (var callbackType : handlerType.getInterfaces()) {
                boolean isRegistered = REGISTRATIONS.stream()
                        .anyMatch(registration -> registration.type().isAssignableFrom(callbackType));
                if (!isRegistered) {
                    throw new IllegalStateException(getClass().getName() + " implements unregistered callback "
                            + callbackType.getName() + "; add it to EventHandler.REGISTRATIONS");
                }
            }
        }
    }

    private <T> void handle(EventRegistration<T> registration) {
        var type = registration.type();
        if (type.isInstance(this)) {
            registration.event().register(type.cast(this));
        }
    }
}
