package org.aesh.command.jbang;

import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Mixin;
import org.aesh.command.option.Option;

@CommandDefinition(name = "info", description = "Provides info about a script")
public class InfoCommand extends BaseCommand {
    @Mixin
    ScriptMixin scriptMixin;

    @Mixin
    DependencyInfoMixin dependencyInfoMixin;

    @Option(name = "tools", hasValue = false, description = "Show resolved tools info")
    boolean tools;

    @Option(name = "classpath", hasValue = false, description = "Show classpath")
    boolean classpath;

    @Override
    public CommandResult execute(CommandInvocation ci) {
        return CommandResult.SUCCESS;
    }
}
