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
import polycube.polycard.commands.*;
import polycube.polycard.data.Storage;
import polycube.polycard.events.CardItemUseEvent;
import polycube.polycard.events.CardLootEvents;
import polycube.polycard.events.callBacks.CardEventCallback;
import polycube.polycard.events.callBacks.ItemUseEventCallback;
import polycube.polycard.events.callBacks.PlayerLoadEventCallback;
import polycube.polycard.utils.CardHelper;
import polycube.polycard.utils.Cooldowns;
import polycube.polycard.utils.Helpers;

import java.util.Objects;

/// Fabric entrypoint that wires storage, commands, callbacks, loot events, and card effects.
public class PolyCard implements ModInitializer {
    public static final String MOD_ID = "polycard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static @Nullable Cooldowns cooldowns;
    private static @Nullable Storage storage;

    /// Returns the cooldown service for the currently running server.
    public static Cooldowns cooldowns() {
        return Objects.requireNonNull(cooldowns, "PolyCard cooldowns are unavailable before the server has started");
    }

    /// Returns persistent card storage for the currently running server.
    public static Storage storage() {
        return Objects.requireNonNull(storage, "PolyCard storage is unavailable before the server has started");
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing PolyCard");
        Helpers.debug("Initialized and validated {} card type(s)", CardType.values().length);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            cooldowns = new Cooldowns();
            storage = Storage.load(server);
            Helpers.debug("Initialized PolyCard state for server {}", server.getServerModName());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(_ -> {
            int discardedTasks = Helpers.clearScheduledTasks();
            cooldowns = null;
            storage = null;
            Helpers.debug("Cleared PolyCard server state and {} pending task(s)", discardedTasks);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            cooldowns().tick();
            Helpers.onServerTick(server);
        });
        PlayerLoadEventCallback.EVENT.register(CardHelper::loadPlayerAttributes);
        ServerPlayerEvents.AFTER_RESPAWN.register((_, newPlayer, _) -> CardHelper.loadPlayerAttributes(newPlayer));
        CardEventCallback.EQUIPPED.register(CardHelper::addCardAttributes);
        CardEventCallback.UNEQUIPPED.register(CardHelper::removeCardAttributes);

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
