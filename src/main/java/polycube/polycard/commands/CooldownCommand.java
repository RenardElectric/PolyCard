package polycube.polycard.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.PolyCard;
import polycube.polycard.utils.Helpers;

public class CooldownCommand extends PolyCardCommand {

    public CooldownCommand() {
        super(
                "cooldown",
                "Gets the currently active cooldowns for a player",
                "",
                PermissionLevel.ALL
        );
    }

    @Override
    protected int execute(CommandSourceStack source) {
        var player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("This command can only be executed by a player."));
            return 0;
        }

        var cooldowns = PolyCard.cooldowns().getCooldownsForPlayer(player);
        var sb = new StringBuilder();
        if (cooldowns.isEmpty()) {
            sb.append("\nNo active cooldowns.");
        } else {
            sb.append("\nActive cooldowns for player ").append(player.getName().getString()).append(":");
            for (var entry : cooldowns.entrySet()) {
                sb.append("\n")
                        .append(Helpers.identifierToTitleCase(entry.getKey()))
                        .append(": ")
                        .append(entry.getValue())
                        .append(" ticks remaining");
            }
        }
        source.sendSuccess(() -> Component.literal(sb.toString()), false);

        return 1;
    }
}
