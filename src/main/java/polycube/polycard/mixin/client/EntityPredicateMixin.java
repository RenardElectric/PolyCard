package polycube.polycard.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Optional;

@Mixin(EntityPredicate.class)
public abstract class EntityPredicateMixin {
    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;withAlternative(Lcom/mojang/serialization/Codec;Lcom/mojang/serialization/Codec;Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"))
    private static Codec<ContextAwarePredicate> polycard$encodeCompactAdvancementPredicate(Codec<ContextAwarePredicate> vanillaCodec) {
        if (System.getProperty("fabric-api.datagen") == null) {
            return vanillaCodec;
        }

        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<ContextAwarePredicate, T>> decode(DynamicOps<T> ops, T input) {
                return vanillaCodec.decode(ops, input);
            }

            @Override
            public <T> DataResult<T> encode(ContextAwarePredicate input, DynamicOps<T> ops, T prefix) {
                var compactPredicate = polycard$unwrapEntityPredicate(input);
                if (compactPredicate.isEmpty()) {
                    return vanillaCodec.encode(input, ops, prefix);
                }

                return EntityPredicate.CODEC.encode(compactPredicate.get(), ops, prefix)
                        .flatMap(encoded -> vanillaCodec.parse(ops, encoded)
                                .map(ignored -> encoded));
            }
        };
    }

    @Unique
    private static Optional<EntityPredicate> polycard$unwrapEntityPredicate(ContextAwarePredicate predicate) {
        List<LootItemCondition> conditions = predicate.conditions;
        if (conditions.size() != 1
                || !(conditions.getFirst() instanceof LootItemEntityPropertyCondition(Optional<EntityPredicate> predicate1, LootContext.EntityTarget entityTarget))
                || entityTarget != LootContext.EntityTarget.THIS) {
            return Optional.empty();
        }

        return predicate1;
    }
}
