package polycube.polycard.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.PolyCard;

public class HelpCommand extends PolyCardCommand {
    public HelpCommand() {
        super(
                "help",
                "Displays a list of available commands and their descriptions.",
                "/" + PolyCard.MOD_ID + " help",
                PermissionLevel.ALL
        );
    }

    @Override
    protected int execute(CommandSourceStack source) {
        StringBuilder helpMessage = new StringBuilder("Available commands:\n");
        for (PolyCardCommand command : PolyCardCommands.getCommands()) {
            if (hasPermission(source, command.getPermissionLevel())) {
                helpMessage.append(command.getUsage())
                        .append("\n")
                        .append("    -")
                        .append(command.getDescription())
                        .append("\n\n");
            }
        }
        source.sendSuccess(() -> Component.literal(helpMessage.toString()), false);
        return 1;
    }
}
