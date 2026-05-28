package org.aesh.command.jbang;

import org.aesh.command.Command;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

public abstract class BaseCommand implements Command<CommandInvocation> {
    @Option(name = "verbose", hasValue = false, description = "Enable verbose output", inherited = true)
    boolean verbose;

    @Option(name = "quiet", hasValue = false, description = "Quiet mode", inherited = true)
    boolean quiet;

    @Option(shortName = 'x', name = "stacktrace", hasValue = false, description = "Print exception stacktraces", inherited = true)
    boolean stacktrace;

    @Option(name = "offline", hasValue = false, description = "Work offline", inherited = true)
    boolean offline;

    @Option(name = "fresh", hasValue = false, description = "Use fresh resources", inherited = true)
    boolean fresh;

    @Option(name = "config", description = "Config file path", inherited = true)
    String configFile;
}
