package polycube.polycard.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.advancements.Advancement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashSet;

@Mixin(Advancement.class)
public abstract class AdvancementMixin {
    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;create(Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"))
    private static Codec<Advancement> polycard$omitDefaultRequirements(Codec<Advancement> vanillaCodec) {
        if (System.getProperty("fabric-api.datagen") == null) {
            return vanillaCodec;
        }

        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<Advancement, T>> decode(DynamicOps<T> ops, T input) {
                return vanillaCodec.decode(ops, input);
            }

            @Override
            public <T> DataResult<T> encode(Advancement input, DynamicOps<T> ops, T prefix) {
                var encoded = vanillaCodec.encode(input, ops, prefix);
                if (!polycard$hasDefaultRequirements(input)) {
                    return encoded;
                }

                return encoded.map(value -> ops.remove(value, "requirements"))
                        .flatMap(value -> vanillaCodec.parse(ops, value)
                                .map(ignored -> value));
            }
        };
    }

    @Unique
    private static boolean polycard$hasDefaultRequirements(Advancement advancement) {
        var criteria = advancement.criteria();
        var requirements = advancement.requirements().requirements();
        if (requirements.size() != criteria.size()) {
            return false;
        }

        var requiredCriteria = new HashSet<String>();
        for (var requirement : requirements) {
            if (requirement.size() != 1) {
                return false;
            }

            var criterion = requirement.getFirst();
            if (!criteria.containsKey(criterion) || !requiredCriteria.add(criterion)) {
                return false;
            }
        }

        return true;
    }
}
