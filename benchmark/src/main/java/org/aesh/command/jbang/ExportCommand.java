package org.aesh.command.jbang;

import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

@CommandDefinition(name = "export", description = "Export a script as a jar or native binary")
public class ExportCommand extends BaseBuildCommand {
    @Option(name = "format", description = "Export format", defaultValue = "jar")
    String format;

    @Option(name = "output", description = "Output file path")
    String outputPath;

    @Option(name = "force", hasValue = false, description = "Force overwrite")
    boolean force;

    @Override
    public CommandResult execute(CommandInvocation ci) {
        return CommandResult.SUCCESS;
    }
}
