package polycube.polycard.commands;

import net.minecraft.server.permissions.PermissionLevel;

public class CombineCommand extends PolyCardCommand {

    public CombineCommand() {
        super(
                "combine",
                "Combine two cards to create a new one with the next rarity level.",
                "",
                PermissionLevel.ALL
        );
    }

}

