package polycube.polycard;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import polycube.polycard.commands.*;
import polycube.polycard.events.callBacks.ItemUseEventCallback;
import polycube.polycard.events.callBacks.PlayerLoadEventCallback;
import polycube.polycard.events.cardDropEvents.CardDropEvents;
import polycube.polycard.cardEffects.CardEffects;
import polycube.polycard.events.guiEvents.CardItemUseEvent;
import polycube.polycard.gui.EquipmentGUI;
import polycube.polycard.manager.CardManager;
import polycube.polycard.utils.Helpers;

/// The main class of the PolyCard mod.
/// It initializes the mod, registers commands, events, and sets up the card manager and equipment GUI.
public class PolyCard implements ModInitializer {
    public static final String MOD_ID = "polycard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static CardManager cardManager = null;

    @Override
    public void onInitialize() {
        Helpers.debug("PolyCard mod initialized.");
        cardManager = new CardManager();
        EquipmentGUI equipmentGUI = new EquipmentGUI(cardManager);

        ServerLifecycleEvents.SERVER_STARTED.register(cardManager::load);
        ServerTickEvents.END_SERVER_TICK.register(Helpers::onServerTick);
        PlayerLoadEventCallback.JOIN.register(cardManager::loadPlayerAttributes);

        PolyCardCommands.registerCommands(
                new HelpCommand(),
                new InfoCommand(),
                new EquipCommand(equipmentGUI),
                new CombineCommand(),
                new GiveCommand(),
                new TestCommand()
        );

        ItemUseEventCallback.register(new CardItemUseEvent(cardManager));

        CardDropEvents.registerCardDropEvents();
        CardEffects.registerCardEffects(cardManager);
    }
}