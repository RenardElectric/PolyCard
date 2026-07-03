package polycube.polycard.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.BlockEvents;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.callBacks.*;

import java.util.Set;

public class EventHandler {
    public record EventRegistration<T>(Class<T> type, Event<T> event) { }
    public static <T> CardEffects.EventRegistration<T> of(Class<T> type, Event<T> event) {
        return new CardEffects.EventRegistration<>(type, event);
    }

    public static final Set<CardEffects.EventRegistration<?>> REGISTRATIONS =  Set.of(
            of(BreedEventCallback.class, BreedEventCallback.EVENT),
            of(EntitySummonedEventCallback.class, EntitySummonedEventCallback.EVENT),
            of(TameEventCallback.class, TameEventCallback.EVENT),

            of(EntityHurtEventCallback.class, EntityHurtEventCallback.EVENT),
            of(ExplosionKnockbackEventCallback.class, ExplosionKnockbackEventCallback.EVENT),
            of(PlayerKillEventCallback.class, PlayerKillEventCallback.EVENT),
            of(ProjectileOnHitEventCallback.class, ProjectileOnHitEventCallback.EVENT),
            of(ServerLivingEntityEvents.AfterDeath.class, ServerLivingEntityEvents.AFTER_DEATH),

            of(IsTargetedEventCallback.class, IsTargetedEventCallback.EVENT),

            of(ItemConsumedEventCallback.class, ItemConsumedEventCallback.EVENT),
            of(ItemDurabilityChangeEventCallback.class, ItemDurabilityChangeEventCallback.EVENT),
            of(ItemUsedEventCallback.class, ItemUsedEventCallback.EVENT),
            of(ItemUseEventCallback.class, ItemUseEventCallback.EVENT),
            of(BlockEvents.UseItemOnCallback.class, BlockEvents.USE_ITEM_ON),

            of(PlayerLoadEventCallback.class, PlayerLoadEventCallback.EVENT),

            of(CardEventCallback.CardEquipEvent.class, CardEventCallback.EQUIPPED),
            of(CardEventCallback.CardUnequipEvent.class, CardEventCallback.UNEQUIPPED),
            of(ServerEntityEvents.EquipmentChange.class, ServerEntityEvents.EQUIPMENT_CHANGE),

            of(ServerTickEvents.EndTick.class, ServerTickEvents.END_SERVER_TICK),
            of(ServerTickEvents.EndLevelTick.class, ServerTickEvents.END_LEVEL_TICK),
            of(PlayerTickEventCallback.class, PlayerTickEventCallback.EVENT)
    );

    public EventHandler() {
        REGISTRATIONS.forEach(this::handle);
    }

    private <T> void handle(CardEffects.EventRegistration<T> registration) {
        var type = registration.type();
        if (type.isInstance(this)) {
            registration.event().register(type.cast(this));
        }
    }
}
