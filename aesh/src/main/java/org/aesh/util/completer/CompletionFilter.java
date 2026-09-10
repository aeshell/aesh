package org.aesh.util.completer;

import java.util.ArrayList;
import java.util.List;

import org.aesh.command.impl.internal.ParsedCommand;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.OptionVisibility;

/**
 * Decides which commands and options are included in a generated static
 * shell completion script.
 * <p>
 * {@link #defaults()} mirrors the dynamic completion behavior: commands whose
 * {@link org.aesh.command.activator.CommandActivator} is off and options that
 * are {@link org.aesh.command.option.OptionVisibility#HIDDEN} or deactivated
 * are excluded. Use {@link #and(CompletionFilter)} to layer custom rules on
 * top, e.g. excluding a platform-specific command group that is unregistered
 * on some builds:
 *
 * <pre>
 * ShellCompletionGenerator.generate(ShellType.BASH, TopCommand.class, "isx",
 *         CompletionFilter.defaults().and(noVmFilter));
 * </pre>
 */
public interface CompletionFilter {

    boolean includeCommand(ProcessedCommand<?, ?> command);

    boolean includeOption(ProcessedCommand<?, ?> command, ProcessedOption option);

    static List<CommandLineParser<? extends CommandInvocation>> visibleChildren(
            CommandLineParser<? extends CommandInvocation> parser, CompletionFilter filter) {
        List<CommandLineParser<? extends CommandInvocation>> children = new ArrayList<>();
        if (parser.isGroupCommand()) {
            for (CommandLineParser<? extends CommandInvocation> child : parser.getAllChildParsers()) {
                if (filter.includeCommand(child.getProcessedCommand()))
                    children.add(child);
            }
        }
        return children;
    }

    static List<ProcessedOption> visibleOptions(ProcessedCommand<?, ?> command, CompletionFilter filter) {
        List<ProcessedOption> options = new ArrayList<>();
        for (ProcessedOption option : command.getOptions()) {
            if (option.isProperty())
                continue;
            if (filter.includeOption(command, option))
                options.add(option);
        }
        return options;
    }

    default CompletionFilter and(CompletionFilter other) {
        CompletionFilter self = this;
        return new CompletionFilter() {
            @Override
            public boolean includeCommand(ProcessedCommand<?, ?> command) {
                return self.includeCommand(command) && other.includeCommand(command);
            }

            @Override
            public boolean includeOption(ProcessedCommand<?, ?> command, ProcessedOption option) {
                return self.includeOption(command, option) && other.includeOption(command, option);
            }
        };
    }

    static CompletionFilter allowAll() {
        return new CompletionFilter() {
            @Override
            public boolean includeCommand(ProcessedCommand<?, ?> command) {
                return true;
            }

            @Override
            public boolean includeOption(ProcessedCommand<?, ?> command, ProcessedOption option) {
                return true;
            }
        };
    }

    static CompletionFilter defaults() {
        return new CompletionFilter() {
            @Override
            public boolean includeCommand(ProcessedCommand<?, ?> command) {
                return command.isActivated(new ParsedCommand(command));
            }

            @Override
            public boolean includeOption(ProcessedCommand<?, ?> command, ProcessedOption option) {
                return option.getVisibility() != OptionVisibility.HIDDEN
                        && option.isActivated(new ParsedCommand(command));
            }
        };
    }
}
