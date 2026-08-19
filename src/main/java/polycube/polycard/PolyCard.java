package polycube.polycard;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import polycube.polycard.card.CardType;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.commands.*;
import polycube.polycard.events.CardItemUseEvent;
import polycube.polycard.events.CardLootEvents;
import polycube.polycard.events.callBacks.ItemUseEventCallback;
import polycube.polycard.utils.Helpers;

import java.util.Objects;

/// Fabric entrypoint that wires storage, commands, callbacks, loot events, and card effects.
public class PolyCard implements ModInitializer {
    public static final String MOD_ID = "polycard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static @Nullable PolyCardRuntime runtime;

    /// Returns the state owner for the currently running server.
    public static PolyCardRuntime runtime() {
        return Objects.requireNonNull(runtime, "PolyCard runtime is unavailable before the server has started");
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing PolyCard");
        CardType.registerEffects();
        Helpers.debug("Initialized and validated {} card type(s)", CardType.values().length);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (runtime != null) throw new IllegalStateException("PolyCard runtime started more than once");
            runtime = PolyCardRuntime.start(server);
            Helpers.debug("Initialized PolyCard state for server {}", server.getServerModName());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(_ -> {
            int discardedTasks = runtime == null ? 0 : runtime.close();
            runtime = null;
            Helpers.debug("Cleared PolyCard server state and {} pending task(s)", discardedTasks);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (runtime != null) runtime.tick(server);
        });
        ServerPlayerEvents.LEAVE.register(CardEffects::clearPlayerState);

        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            return ItemUseEventCallback.EVENT.invoker().onItemUse(serverPlayer, level, hand);
        });

        PolyCardCommands.registerCommands(
                new HelpCommand(),
                new InfoCommand(),
                new EquipCommand(),
                new CombineCommand(),
                new GiveCommand(),
                new TestCommand(),
                new CooldownCommand()
        );

        new CardItemUseEvent().registerCallbacks();
        new CardLootEvents().registerCallbacks();
    }
}
