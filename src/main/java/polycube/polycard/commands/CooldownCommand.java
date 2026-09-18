package polycube.polycard.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;
import polycube.polycard.PolyCard;
import polycube.polycard.utils.Helpers;
import polycube.polycore.commands.PolyCommand;
import polycube.polycore.text.TextComponents;

public class CooldownCommand extends PolyCommand {

    public CooldownCommand() {
        super(
                PolyCard.MOD_ID,
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
            source.sendFailure(TextComponents.error("This command can only be executed by a player."));
            return 0;
        }

        var cooldowns = PolyCard.runtime().cooldowns().getCooldownsForPlayer(player);
        var message = TextComponents.header("Cooldowns")
                .append(TextComponents.field("Player", TextComponents.value(player.getName())));
        if (cooldowns.isEmpty()) {
            message.append(TextComponents.field("Active cooldowns", TextComponents.muted("None")));
        } else {
            message.append(TextComponents.field("Active cooldowns", TextComponents.value(cooldowns.size())));
            for (var entry : cooldowns.entrySet()) {
                var remainingTicks = entry.getValue();
                var tickLabel = remainingTicks == 1 ? " tick remaining" : " ticks remaining";
                message.append(TextComponents.indentedField(
                        Helpers.identifierToTitleCase(entry.getKey()),
                        TextComponents.value(remainingTicks).append(TextComponents.muted(tickLabel))
                ));
            }
        }
        source.sendSuccess(() -> message, false);

        return 1;
    }
}
