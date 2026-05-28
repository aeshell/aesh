package org.aesh.command.jbang;

import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;

@CommandDefinition(name = "jdk", description = "Manage JDK installations")
public class JdkCommand extends BaseCommand {
    @Option(name = "install", hasValue = false, description = "Install a JDK")
    boolean install;

    @Option(name = "list", hasValue = false, description = "List installed JDKs")
    boolean list;

    @Option(name = "default-version", description = "Set default JDK version")
    String defaultVersion;

    @Argument(description = "JDK version")
    String version;

    @Override
    public CommandResult execute(CommandInvocation ci) {
        return CommandResult.SUCCESS;
    }
}
