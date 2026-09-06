package polycube.polycard.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.PolyCard;
import polycube.polycard.utils.Helpers;

public class CooldownCommand extends PolyCardCommand {

    public CooldownCommand() {
        super(
                "cooldown",
                "Show your active card-effect cooldowns",
                "",
                PermissionLevel.ALL,
                true
        );
    }

    @Override
    protected int execute(CommandSourceStack source) {
        var player = source.getPlayer();

        if (player == null) {
            source.sendFailure(CommandText.error("This command can only be executed by a player."));
            return 0;
        }

        var cooldowns = PolyCard.runtime().cooldowns().getCooldownsForPlayer(player);
        var message = CommandText.header("Cooldowns")
                .append(CommandText.field("Player", CommandText.value(player.getName())));
        if (cooldowns.isEmpty()) {
            message.append(CommandText.field("Active cooldowns", CommandText.muted("None")));
        } else {
            message.append(CommandText.field("Active cooldowns", CommandText.value(cooldowns.size())));
            for (var entry : cooldowns.entrySet()) {
                var remainingTicks = entry.getValue();
                var tickLabel = remainingTicks == 1 ? " tick remaining" : " ticks remaining";
                message.append(CommandText.indentedField(
                        Helpers.identifierToTitleCase(entry.getKey()),
                        CommandText.value(remainingTicks).append(CommandText.muted(tickLabel))
                ));
            }
        }
        source.sendSuccess(() -> message, false);

        return 1;
    }
}
