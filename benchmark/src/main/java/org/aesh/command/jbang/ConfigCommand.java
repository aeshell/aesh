package org.aesh.command.jbang;

import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;

@CommandDefinition(name = "config", description = "Read and write configuration options")
public class ConfigCommand extends BaseCommand {
    @Option(name = "get", hasValue = false, description = "Get a config value")
    boolean get;

    @Option(name = "set", hasValue = false, description = "Set a config value")
    boolean set;

    @Option(name = "unset", hasValue = false, description = "Unset a config value")
    boolean unset;

    @Option(name = "list", hasValue = false, description = "List config values")
    boolean list;

    @Argument(description = "Config key")
    String key;

    @Override
    public CommandResult execute(CommandInvocation ci) {
        return CommandResult.SUCCESS;
    }
}
