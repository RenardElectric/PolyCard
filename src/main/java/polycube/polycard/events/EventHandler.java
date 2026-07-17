package polycube.polycard.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.BlockEvents;
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
            of(ServerLivingEntityEvents.AfterDeath.class, ServerLivingEntityEvents.AFTER_DEATH),

            of(IsTargetedEventCallback.class, IsTargetedEventCallback.EVENT),

            of(ItemConsumedEventCallback.class, ItemConsumedEventCallback.EVENT),
            of(ItemDurabilityChangeEventCallback.class, ItemDurabilityChangeEventCallback.EVENT),
            of(FallFlyingGliderWearEventCallback.class, FallFlyingGliderWearEventCallback.EVENT),
            of(ItemUseEventCallback.class, ItemUseEventCallback.EVENT),
            of(BlockEvents.UseItemOnCallback.class, BlockEvents.USE_ITEM_ON),

            of(ServerPlayerEvents.Join.class, ServerPlayerEvents.JOIN),
            of(ServerPlayerEvents.AfterRespawn.class, ServerPlayerEvents.AFTER_RESPAWN),
            of(ServerPlayerEvents.Leave.class, ServerPlayerEvents.LEAVE),

            of(CardEventCallback.CardEquipEvent.class, CardEventCallback.EQUIPPED),
            of(CardEventCallback.CardUnequipEvent.class, CardEventCallback.UNEQUIPPED),
            of(ServerEntityEvents.EquipmentChange.class, ServerEntityEvents.EQUIPMENT_CHANGE),

            of(ServerTickEvents.EndLevelTick.class, ServerTickEvents.END_LEVEL_TICK),
            of(PlayerTickEventCallback.class, PlayerTickEventCallback.EVENT)
    );

    private boolean eventsRegistered;

    protected EventHandler() {
    }

    /// Registers every callback interface implemented by this fully constructed handler.
    public final void registerCallbacks() {
        if (eventsRegistered) {
            throw new IllegalStateException(getClass().getName() + " registered its events more than once");
        }
        eventsRegistered = true;
        REGISTRATIONS.forEach(this::handle);
    }

    private <T> void handle(EventRegistration<T> registration) {
        var type = registration.type();
        if (type.isInstance(this)) {
            registration.event().register(type.cast(this));
        }
    }
}
