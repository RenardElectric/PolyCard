package polycube.polycard;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import polycube.polycard.cardEffects.hostile.EnderDragonEffects;
import polycube.polycard.cardEffects.hostile.WitherEffects;
import polycube.polycard.cardEffects.hostile.ZombieEffects;
import polycube.polycard.cardEffects.neutral.*;
import polycube.polycard.cardEffects.passive.ChickenEffects;
import polycube.polycard.cardEffects.passive.CowEffects;
import polycube.polycard.cardEffects.passive.SquidEffects;
import polycube.polycard.commands.*;
import polycube.polycard.data.Storage;
import polycube.polycard.events.callBacks.CardEventCallback;
import polycube.polycard.events.callBacks.ItemUseEventCallback;
import polycube.polycard.events.callBacks.PlayerLoadEventCallback;
import polycube.polycard.events.cardLootEvents.BreedEvents;
import polycube.polycard.events.cardLootEvents.KillEvents;
import polycube.polycard.events.cardLootEvents.SummonEvents;
import polycube.polycard.events.cardLootEvents.UseItemOnEvents;
import polycube.polycard.events.guiEvents.CardItemUseEvent;
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
        PlayerLoadEventCallback.JOIN.register(CardHelper::loadPlayerAttributes);
        CardEventCallback.EQUIPPED.register(CardHelper::addCardAttributes);
        CardEventCallback.UNEQUIPPED.register(CardHelper::removeCardAttributes);

        PolyCardCommands.registerCommands(
                new HelpCommand(),
                new InfoCommand(),
                new EquipCommand(),
                new CombineCommand(),
                new GiveCommand(),
                new TestCommand()
        );

        ItemUseEventCallback.register(new CardItemUseEvent());

        // Card loot events
        BreedEvents.register();
        KillEvents.register();
        SummonEvents.register();
        UseItemOnEvents.register();

        // Card effects
        // Passive
        CowEffects.register();
        SquidEffects.register();
        BeeEffects.register();
        ChickenEffects.register();

        // Neutral
        EnderManEffects.register();
        IronGolemEffects.register();
        PiglinEffects.register();
        ZombifiedPiglinEffects.register();

        // Hostile
        ZombieEffects.register();
        WitherEffects.register();
        EnderDragonEffects.register();
    }
}
