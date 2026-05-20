package polycube.polycard.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import polycube.polycard.events.callBacks.LootParamsCreationEventCallback;

import java.util.Map;

@Mixin(LootParams.Builder.class)
public class LootParamsBuilderMixin {

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private ContextMap.Builder params;

    @Shadow
    @Final
    private Map<Identifier, LootParams.DynamicDrop> dynamicDrops;

    @Shadow
    private float luck;

    @WrapMethod(method = "create")
    public LootParams create(ContextKeySet contextKeySet, Operation<LootParams> original) {
        LootParamsCreationEventCallback.EVENT.invoker().interact(
                (LootParams.Builder) (Object) this,
                new LootParamsCreationEventCallback.LootParamsBuilderData(level, params, dynamicDrops, luck)
        );
        return original.call(contextKeySet);
    }
}
