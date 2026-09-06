package polycube.polycard.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.PolyCard;

public class HelpCommand extends PolyCardCommand {
    public HelpCommand() {
        super(
                "help",
                "List the commands available to you",
                "",
                PermissionLevel.ALL
        );
    }

    @Override
    protected int execute(CommandSourceStack source) {
        var helpMessage = CommandText.header("Commands")
                .append("\nClick a command to put it in chat, or open its details for syntax and shortcuts.");
        for (PolyCardCommand command : PolyCardCommands.getCommands()) {
            if (hasPermission(source, command.getPermissionLevel())) {
                String root = "/" + PolyCard.MOD_ID + " " + command.getName();
                helpMessage.append("\n\n  ").append(CommandText.action(root, root + " "));
                if (command != this) {
                    helpMessage.append(" ").append(CommandText.action("[Details]", root + " help"));
                }
                if (command.getPermissionLevel() != PermissionLevel.ALL) {
                    helpMessage.append(CommandText.muted(" (Gamemasters only)"));
                }
                helpMessage.append("\n  " + command.getDescription());
            }
        }
        source.sendSuccess(() -> helpMessage, false);
        return 1;
    }
}
