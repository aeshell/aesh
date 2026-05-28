package org.aesh.command.jbang;

import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;

@CommandDefinition(name = "alias", description = "Manage aliases for scripts")
public class AliasCommand extends BaseCommand {
    @Option(name = "add", hasValue = false, description = "Add a new alias")
    boolean add;

    @Option(name = "remove", hasValue = false, description = "Remove an alias")
    boolean remove;

    @Option(name = "list", hasValue = false, description = "List aliases")
    boolean list;

    @Argument(description = "Alias name")
    String name;

    @Override
    public CommandResult execute(CommandInvocation ci) {
        return CommandResult.SUCCESS;
    }
}
