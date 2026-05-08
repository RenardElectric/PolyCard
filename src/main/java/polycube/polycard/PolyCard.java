package polycube.polycard;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import polycube.polycard.commands.*;
import polycube.polycard.events.cardDropEvents.CardDropEvents;
import polycube.polycard.events.cardEvents.CardEvents;
import polycube.polycard.events.guiEvents.CardItemUseEvent;
import polycube.polycard.events.callBacks.ItemUseEvents;
import polycube.polycard.gui.EquipmentGUI;
import polycube.polycard.manager.CardManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class PolyCard implements ModInitializer {
	public static final String MOD_ID = "polycard";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final List<ScheduledTask> TASKS = new ArrayList<>();

    @Override
	public void onInitialize() {
		LOGGER.debug("PolyCard mod initialized.");
        CardManager cardManager = new CardManager();
        EquipmentGUI equipmentGUI = new EquipmentGUI(cardManager.getStorage());

		ServerTickEvents.END_SERVER_TICK.register(PolyCard::onServerTick);

		PolyCardCommands.registerCommands(
				new HelpCommand(),
				new GiveCardCommand(),
				new TestCommand(),
				new EquipCommand(equipmentGUI)
		);

		ItemUseEvents.registerItemUseEvents(
				new CardItemUseEvent(cardManager)
		);

		CardDropEvents.registerCardDropEvents();
		CardEvents.registerCardEvents(cardManager);
	}


	private static void onServerTick(MinecraftServer server) {
		Iterator<ScheduledTask> iterator = TASKS.iterator();

		while (iterator.hasNext()) {
			ScheduledTask task = iterator.next();

			task.ticksLeft--;

			if (task.ticksLeft <= 0) {
				task.runnable.accept(server);
				if (task instanceof TaskTimer timer && timer.isRepeating) {
					task.ticksLeft = timer.period;
				} else {
					iterator.remove();
				}
			}
		}
	}

	public static void runLater(int ticks, Consumer<MinecraftServer> runnable) {
		TASKS.add(new ScheduledTask(ticks, runnable));
	}

	public static void runTaskTimer(int delay, int period, Consumer<MinecraftServer> runnable) {
		TASKS.add(new TaskTimer(delay, runnable, true, period));
	}

	private static class ScheduledTask {
		int ticksLeft;
		Consumer<MinecraftServer> runnable;

		ScheduledTask(int ticksLeft, Consumer<MinecraftServer> runnable) {
			this.ticksLeft = ticksLeft;
			this.runnable = runnable;
		}
	}

	private static class TaskTimer extends ScheduledTask {
		boolean isRepeating;
		int period;

		TaskTimer(int ticksLeft, Consumer<MinecraftServer> runnable, Boolean isRepeating, int period) {
			super(ticksLeft, runnable);
			this.isRepeating = isRepeating;
			this.period = period;
		}
	}
}