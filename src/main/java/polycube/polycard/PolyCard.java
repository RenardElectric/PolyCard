package polycube.polycard;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import polycube.polycard.commands.*;
import polycube.polycard.gui.EquipmentGUI;
import polycube.polycard.manager.CardManager;

public class PolyCard implements ModInitializer {
	public static final String MOD_ID = "polycard";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private CardManager cardManager;
	private EquipmentGUI equipmentGUI;

	@Override
	public void onInitialize() {
		LOGGER.info("PolyCard mod initialized.");
		cardManager = new CardManager();
		equipmentGUI = new EquipmentGUI(cardManager.getStorage(), cardManager);

		PolyCardCommands.registerCommands(
				new HelpCommand(),
				new GiveCardCommand(cardManager),
				new TestCommand(cardManager),
				new EquipCommand(equipmentGUI)
		);
	}
}