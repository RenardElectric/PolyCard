package polycube.polycard;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import polycube.polycard.commands.*;
import polycube.polycard.data.Storage;
import polycube.polycard.events.CardLootEvents;
import polycube.polycard.events.callBacks.CardEventCallback;
import polycube.polycard.events.callBacks.ItemUseEventCallback;
import polycube.polycard.events.callBacks.PlayerLoadEventCallback;
import polycube.polycard.events.CardItemUseEvent;
import polycube.polycard.utils.CardHelper;
import polycube.polycard.utils.Cooldowns;
import polycube.polycard.utils.Helpers;

/// Fabric entrypoint that wires storage, commands, callbacks, loot events, and card effects.
public class PolyCard implements ModInitializer {
    public static final String MOD_ID = "polycard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Cooldowns COOLDOWNS = null;
    public static Storage STORAGE = null;

    @Override
    public void onInitialize() {
        Helpers.debug("PolyCard mod initialized.");

        Helpers.runTaskTimer(0, 1, _ -> COOLDOWNS.tick());

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            COOLDOWNS = new Cooldowns();
            STORAGE = Storage.load(server);
        });
        ServerTickEvents.END_SERVER_TICK.register(Helpers::onServerTick);
        PlayerLoadEventCallback.EVENT.register(CardHelper::loadPlayerAttributes);
        CardEventCallback.EQUIPPED.register(CardHelper::addCardAttributes);
        CardEventCallback.UNEQUIPPED.register(CardHelper::removeCardAttributes);

        UseItemCallback.EVENT.register(((player, level, hand) -> {
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }
            return ItemUseEventCallback.EVENT.invoker().onItemUse((ServerPlayer) player, level, hand);
        }));

        PolyCardCommands.registerCommands(
                new HelpCommand(),
                new InfoCommand(),
                new EquipCommand(),
                new CombineCommand(),
                new GiveCommand(),
                new TestCommand(),
                new CooldownCommand()
        );

        new CardItemUseEvent();
        new CardLootEvents();
    }
}
