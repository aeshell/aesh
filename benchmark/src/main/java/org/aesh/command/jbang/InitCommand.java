package org.aesh.command.jbang;

import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Mixin;
import org.aesh.command.option.Option;

@CommandDefinition(name = "init", description = "Initialize a new script")
public class InitCommand extends BaseCommand {
    @Mixin
    DependencyInfoMixin dependencyInfoMixin;

    @Option(name = "template", description = "Template to use", defaultValue = "hello")
    String template;

    @Option(name = "force", hasValue = false, description = "Force overwrite")
    boolean force;

    @Argument(description = "Script file to create")
    String scriptFile;

    @Override
    public CommandResult execute(CommandInvocation ci) {
        return CommandResult.SUCCESS;
    }
}
