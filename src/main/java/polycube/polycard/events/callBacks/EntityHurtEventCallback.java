package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/// Callback for an entity being hurt.
/// Called before the hurt damage is applied.
///
/// Upon return:
/// - SUCCESS cancels further processing and continues with normal hurt effects (e.g. damage, armor reduction, enchantments, etc.)
/// - PASS falls back to further processing and defaults to SUCCESS if no other listeners are available
/// - FAIL cancels further processing and prevents any hurt damage from being applied
public interface EntityHurtEventCallback {
    Event<EntityHurtEventCallback> EVENT = EventFactory.createArrayBacked(EntityHurtEventCallback.class,
            (listeners) -> (instance, level, source) -> {
                for (EntityHurtEventCallback listener : listeners) {
                    InteractionResult result = listener.interact(instance, level, source);

                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            });

    InteractionResult interact(LivingEntity instance, ServerLevel level, DamageSource source);
}
