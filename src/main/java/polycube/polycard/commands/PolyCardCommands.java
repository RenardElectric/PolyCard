package polycube.polycard.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import polycube.polycard.PolyCard;

public class PolyCardCommands {
    private static PolyCardCommand[] commands;

    public static void registerCommands(PolyCardCommand... commands) {
        PolyCardCommands.commands = commands;
        CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> {
            var baseCommand = Commands.literal(PolyCard.MOD_ID);
            baseCommand.executes(_ -> printModInfo());
            for (PolyCardCommand command : commands) {
                baseCommand.then(command.getCommand());
            }
            dispatcher.register(baseCommand);
        });
    }

    private static int printModInfo() {
        PolyCard.LOGGER.info("PolyCard Version 1.0.0 - A Minecraft mod for card-based gameplay mechanics."); // TODO: Get infos
        return 1;
    }

    public static PolyCardCommand[] getCommands() {
        return commands;
    }
}
