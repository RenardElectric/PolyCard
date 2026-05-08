package polycube.polycard.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import polycube.polycard.PolyCard;

public class PolyCardCommands {
    private static PolyCardCommand[] commands;

    public static void registerCommands(PolyCardCommand... commands) {
        PolyCardCommands.commands = commands;
        CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> {
            var baseCommand = Commands.literal(PolyCard.MOD_ID);
            baseCommand.executes(context -> printModInfo(context.getSource()));
            for (PolyCardCommand command : commands) {
                baseCommand.then(command.getCommand());
            }
            dispatcher.register(baseCommand);
        });
    }

    private static int printModInfo(CommandSourceStack cst) {
        cst.sendSuccess(() -> Component.literal("PolyCard Version 1.0.0\nA Minecraft mod for card-based gameplay mechanics."), false);
        return 1;
    }

    public static PolyCardCommand[] getCommands() {
        return commands;
    }
}
