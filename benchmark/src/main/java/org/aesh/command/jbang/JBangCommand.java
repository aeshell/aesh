package org.aesh.command.jbang;

import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

@GroupCommandDefinition(name = "jbang", description = "JBang CLI tool", groupCommands = {
        RunCommand.class, BuildCommand.class, EditCommand.class,
        InitCommand.class, ExportCommand.class, InfoCommand.class,
        AliasCommand.class, CacheCommand.class, ConfigCommand.class,
        JdkCommand.class, VersionCommand.class
}, generateHelp = true, version = "0.121.0")
public class JBangCommand extends BaseCommand {
    @Option(shortName = 'V', name = "show-version", hasValue = false, description = "Display version info")
    boolean versionRequested;

    @Option(name = "preview", hasValue = false, description = "Enable preview features")
    boolean preview;

    @Override
    public CommandResult execute(CommandInvocation ci) {
        return CommandResult.SUCCESS;
    }
}
