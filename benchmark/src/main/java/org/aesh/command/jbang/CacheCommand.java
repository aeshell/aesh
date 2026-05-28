package org.aesh.command.jbang;

import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

@CommandDefinition(name = "cache", description = "Manage caches")
public class CacheCommand extends BaseCommand {
    @Option(name = "clear", hasValue = false, description = "Clear the cache")
    boolean clear;

    @Option(name = "list", hasValue = false, description = "List cache entries")
    boolean list;

    @Override
    public CommandResult execute(CommandInvocation ci) {
        return CommandResult.SUCCESS;
    }
}
