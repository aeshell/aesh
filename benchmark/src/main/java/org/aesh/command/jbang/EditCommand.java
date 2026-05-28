package org.aesh.command.jbang;

import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

@CommandDefinition(name = "edit", description = "Setup a temporary project to edit script in IDE")
public class EditCommand extends BaseBuildCommand {
    @Option(name = "open", description = "Open editor", defaultValue = "code")
    String open;

    @Option(name = "live", hasValue = false, description = "Enable live reload")
    boolean live;

    @Override
    public CommandResult execute(CommandInvocation ci) {
        return CommandResult.SUCCESS;
    }
}
