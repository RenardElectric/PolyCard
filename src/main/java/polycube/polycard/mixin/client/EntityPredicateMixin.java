package polycube.polycard.mixin.client;

import net.minecraft.advancements.predicates.entity.EntityPredicate;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityPredicate.class)
public abstract class EntityPredicateMixin {
//    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;withAlternative(Lcom/mojang/serialization/Codec;Lcom/mojang/serialization/Codec;Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"))
//    private static Codec<ContextAwarePredicate> polycard$encodeCompactAdvancementPredicate(Codec<ContextAwarePredicate> vanillaCodec) {
//        if (System.getProperty("fabric-api.datagen") == null) {
//            return vanillaCodec;
//        }
//
//        return new Codec<>() {
//            @Override
//            public <T> DataResult<Pair<ContextAwarePredicate, T>> decode(DynamicOps<T> ops, T input) {
//                return vanillaCodec.decode(ops, input);
//            }
//
//            @Override
//            public <T> DataResult<T> encode(ContextAwarePredicate input, DynamicOps<T> ops, T prefix) {
//                var compactPredicate = polycard$unwrapEntityPredicate(input);
//                if (compactPredicate.isEmpty()) {
//                    return vanillaCodec.encode(input, ops, prefix);
//                }
//
//                return EntityPredicate.CODEC.encode(compactPredicate.get(), ops, prefix)
//                        .flatMap(encoded -> vanillaCodec.parse(ops, encoded)
//                                .map(ignored -> encoded));
//            }
//        };
//    }
//
//    @Unique
//    private static Optional<EntityPredicate> polycard$unwrapEntityPredicate(ContextAwarePredicate predicate) {
//        List<LootItemCondition> conditions = predicate.conditions;
//        if (conditions.size() != 1
//                || !(conditions.getFirst() instanceof LootItemEntityPropertyCondition(Optional<EntityPredicate> predicate1, LootContext.EntityTarget entityTarget))
//                || entityTarget != LootContext.EntityTarget.THIS) {
//            return Optional.empty();
//        }
//
//        return predicate1;
//    }
}
