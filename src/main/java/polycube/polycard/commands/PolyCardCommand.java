package polycube.polycard.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

public abstract class PolyCardCommand {
    private final String name;
    private final String description;
    private final String usage;
    private final PermissionLevel permissionLevel;

    public PolyCardCommand(String name, String description, String usage, PermissionLevel permissionLevel) { //TODO luck perm
        this.name = name;
        this.description = description;
        this.usage = usage;
        this.permissionLevel = permissionLevel;
    }

    protected String getName()  {
        return name;
    }

    protected String getDescription()   {
        return description;
    }

    protected String getUsage()  {
        return usage;
    }

    protected PermissionLevel getPermissionLevel() {
        return this.permissionLevel;
    }

    public ArgumentBuilder<CommandSourceStack, ?> getCommand() {
        return Commands.literal(name)
                .requires(source -> hasPermission(source, permissionLevel))
                .executes(e -> execute(e.getSource()));
    }

    protected boolean hasPermission(CommandSourceStack source, PermissionLevel permissionLevel) {
        return source.permissions().hasPermission(new Permission.HasCommandLevel(permissionLevel));
    }

    protected int execute(CommandSourceStack source) {
        return 0;
    }
}
