package polycube.polycard.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.Person;
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

    public static int printModInfo(CommandSourceStack cst) {
        var optionalModData = FabricLoader.getInstance()
                .getModContainer(PolyCard.MOD_ID)
                .map(ModContainer::getMetadata);

        if (optionalModData.isEmpty()) {
            cst.sendFailure(Component.literal("Could not fetch mod information."));
            return 0;
        }
        var modData = optionalModData.get();
        var modInfo = Component.literal("\n" + modData.getName() + " v" + modData.getVersion().getFriendlyString())
                .append("\nMade by " + modData.getAuthors().stream().map(Person::getName).reduce((a, b) -> a + " and " + b).orElse("Unknown authors"))
                .append("\n" + modData.getDescription());
        cst.sendSuccess(() -> modInfo, false);
        return 1;
    }

    public static PolyCardCommand[] getCommands() {
        return commands;
    }
}
