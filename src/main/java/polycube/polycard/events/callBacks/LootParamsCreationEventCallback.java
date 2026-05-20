package polycube.polycard.events.callBacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.Map;

public interface LootParamsCreationEventCallback {
    Event<LootParamsCreationEventCallback> EVENT = EventFactory.createArrayBacked(LootParamsCreationEventCallback.class,
            (listeners) -> (paramsBuilder, builderData) -> {
                for (var listener : listeners) {
                    listener.interact(paramsBuilder, builderData);
                }
            });

    void interact(LootParams.Builder paramsBuilder, LootParamsBuilderData builderData);

    record LootParamsBuilderData(ServerLevel level, ContextMap.Builder params, Map<Identifier, LootParams.DynamicDrop> dynamicDrops, float luck) { }
}
