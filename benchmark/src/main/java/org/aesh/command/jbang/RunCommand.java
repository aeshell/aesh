package org.aesh.command.jbang;

import java.util.List;

import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Mixin;
import org.aesh.command.option.Option;

@CommandDefinition(name = "run", description = "Builds and runs provided script", stopAtFirstPositional = true)
public class RunCommand extends BaseBuildCommand {
    @Mixin
    RunMixin runMixin;

    @Option(shortName = 'c', name = "code", description = "Run given string as code")
    String literalScript;

    @Arguments(description = "Parameters to pass to the script", index = "1..*")
    List<String> userParams;

    @Override
    public CommandResult execute(CommandInvocation ci) {
        return CommandResult.SUCCESS;
    }
}
