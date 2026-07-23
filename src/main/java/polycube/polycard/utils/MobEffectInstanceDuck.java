package polycube.polycard.utils;

import org.jspecify.annotations.Nullable;

/** Internal ownership marker carried by server-side managed effect instances. */
public interface MobEffectInstanceDuck {
    @Nullable Object polycard$getPersistentEffectOwner();

    void polycard$setPersistentEffectOwner(@Nullable Object owner);
}
