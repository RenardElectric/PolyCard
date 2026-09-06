package polycube.polycard.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.jspecify.annotations.Nullable;
import polycube.polycard.PolyCard;

import java.util.Objects;

public final class PolyCardCommands {
    private static PolyCardCommand @Nullable [] commands;

    private PolyCardCommands() {}

    public static void registerCommands(PolyCardCommand... commands) {
        PolyCardCommands.commands = commands;
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, _) -> {
            var baseCommand = Commands.literal(PolyCard.MOD_ID);
            baseCommand.executes(context -> printModInfo(context.getSource()));
            for (PolyCardCommand command : commands) {
                for (var commandAlias : command.getCommands(buildContext)) {
                    baseCommand.then(commandAlias);
                    if (command.hasQuickAlias()) dispatcher.register(commandAlias);
                }
            }
            dispatcher.register(baseCommand);
            PolyCard.LOGGER.debug("Registered {} PolyCard subcommand(s)", commands.length);
        });
    }

    public static int printModInfo(CommandSourceStack cst) {
        var optionalModData = FabricLoader.getInstance()
                .getModContainer(PolyCard.MOD_ID)
                .map(ModContainer::getMetadata);

        if (optionalModData.isEmpty()) {
            PolyCard.LOGGER.warn("Could not find PolyCard metadata while handling the base command");
            cst.sendFailure(CommandText.error("Could not fetch mod information."));
            return 0;
        }
        var modData = optionalModData.get();
        var authors = modData.getAuthors().stream()
                .map(Person::getName)
                .reduce((a, b) -> a + " and " + b)
                .orElse("Unknown authors");
        var modInfo = CommandText.header(modData.getName())
                .append(CommandText.muted(" v" + modData.getVersion().getFriendlyString()))
                .append(CommandText.field("Authors", CommandText.value(authors)))
                .append("\n" + modData.getDescription())
                .append("\n").append(CommandText.action("[View commands]", "/" + PolyCard.MOD_ID + " help"));
        cst.sendSuccess(() -> modInfo, false);
        return 1;
    }

    public static PolyCardCommand[] getCommands() {
        return Objects.requireNonNull(commands, "PolyCard commands are unavailable before registration");
    }
}
