package polycube.polycard;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import polycube.polycard.commands.*;
import polycube.polycard.events.breedEvents.BreedEvents;
import polycube.polycard.events.breedEvents.CowBreedEvent;
import polycube.polycard.events.itemEvents.CardItemEvent;
import polycube.polycard.events.itemEvents.ItemEvents;
import polycube.polycard.events.killEvents.KillEvents;
import polycube.polycard.gui.EquipmentGUI;
import polycube.polycard.manager.CardManager;

public class PolyCard implements ModInitializer {
	public static final String MOD_ID = "polycard";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
	public void onInitialize() {
		LOGGER.debug("PolyCard mod initialized.");
        CardManager cardManager = new CardManager();
        EquipmentGUI equipmentGUI = new EquipmentGUI(cardManager.getStorage());

		PolyCardCommands.registerCommands(
				new HelpCommand(),
				new GiveCardCommand(),
				new TestCommand(cardManager),
				new EquipCommand(equipmentGUI)
		);

		ItemEvents.registerItemEvents(
				new CardItemEvent(cardManager)
		);

		BreedEvents.registerBreedEvents();
		KillEvents.registerKillEvents();
	}
}