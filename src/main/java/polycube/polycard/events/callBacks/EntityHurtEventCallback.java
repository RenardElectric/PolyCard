package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.concurrent.atomic.AtomicLong;

/// Callback for an entity being hurt.
/// Called before the hurt damage is applied.
///
/// Upon return:
/// - SUCCESS cancels further processing and continues with normal hurt effects (e.g. damage, armor reduction, enchantments, etc.)
/// - PASS falls back to further processing and defaults to SUCCESS if no other listeners are available
/// - FAIL cancels further processing and prevents any hurt damage from being applied
public interface EntityHurtEventCallback {
    Event<EntityHurtEventCallback> EVENT = EventFactory.createArrayBacked(EntityHurtEventCallback.class,
            (listeners) -> (instance, level, source, damage) -> {
                for (var listener : listeners) {
                    InteractionResult result = listener.interact(instance, level, source, damage);

                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            });

    InteractionResult interact(LivingEntity instance, ServerLevel level, DamageSource source, AtomicDouble damage);

    class AtomicDouble extends Number {

        private final AtomicLong bits;

        public AtomicDouble() {
            this(0.0);
        }

        public AtomicDouble(double initialValue) {
            bits = new AtomicLong(Double.doubleToLongBits(initialValue));
        }

        public final boolean compareAndSet(double expect, double update) {
            return bits.compareAndSet(Double.doubleToLongBits(expect), Double.doubleToLongBits(update));
        }

        public final void set(double newValue) {
            bits.set(Double.doubleToLongBits(newValue));
        }

        public final double get() {
            return Double.longBitsToDouble(bits.get());
        }

        public final double getAndSet(double newValue) {
            return Double.longBitsToDouble(bits.getAndSet(Double.doubleToLongBits(newValue)));
        }

        public float floatValue()   { return (float) get(); }
        public double doubleValue() { return get();         }
        public int intValue()       { return (int) get();   }
        public long longValue()     { return (long) get();  }
    }
}