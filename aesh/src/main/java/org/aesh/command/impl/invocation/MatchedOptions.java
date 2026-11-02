package org.aesh.command.impl.invocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;

/**
 * Shared implementation of the {@code CommandInvocation} matched-option
 * queries ({@code hasMatchedOption} and friends, #707). Both invocation
 * implementations delegate here so console and default runtimes behave the
 * same. Matching is name-based (long name with or without dashes, short
 * name, alias) against explicitly given options along the selected parser
 * path; the innermost command shadows outer ones.
 */
final class MatchedOptions {

    private MatchedOptions() {
    }

    static boolean hasMatchedOption(CommandContainer<? extends CommandInvocation> container, String name) {
        return findMatchedOption(container, name) != null;
    }

    static List<String> matchedOptionValues(CommandContainer<? extends CommandInvocation> container, String name) {
        ProcessedOption option = findMatchedOption(container, name);
        if (option == null)
            return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(option.getValues()));
    }

    static Set<String> matchedOptionNames(CommandContainer<? extends CommandInvocation> container) {
        Set<String> names = new LinkedHashSet<>();
        for (CommandLineParser<?> parser : selectedPath(container)) {
            for (ProcessedOption option : parser.getProcessedCommand().getOptions()) {
                if (option.isExplicitlyGiven() && option.name() != null && !option.name().isEmpty())
                    names.add(option.name());
            }
        }
        return Collections.unmodifiableSet(names);
    }

    private static ProcessedOption findMatchedOption(CommandContainer<? extends CommandInvocation> container,
            String name) {
        if (name == null || name.isEmpty())
            return null;
        String stripped = name;
        while (stripped.startsWith("-"))
            stripped = stripped.substring(1);
        if (stripped.isEmpty())
            return null;
        List<CommandLineParser<?>> path = selectedPath(container);
        for (int i = path.size() - 1; i >= 0; i--) {
            for (ProcessedOption option : path.get(i).getProcessedCommand().getOptions()) {
                if (!option.isExplicitlyGiven())
                    continue;
                if (stripped.equals(option.name())
                        || stripped.equals(option.shortName())
                        || option.getAliases().contains(stripped))
                    return option;
            }
        }
        return null;
    }

    /**
     * The selected parser path from the root group down to the executed
     * command, using only the public parser contract.
     */
    private static List<CommandLineParser<?>> selectedPath(
            CommandContainer<? extends CommandInvocation> container) {
        List<CommandLineParser<?>> path = new ArrayList<>();
        if (container == null)
            return path;
        CommandLineParser<?> parser = container.getParser();
        if (parser == null)
            return path;
        path.add(parser);
        while (true) {
            if (parser.parsedCommand() == parser)
                break;
            CommandLineParser<?> next = null;
            if (parser.isGroupCommand() && parser.getChildParsers() != null) {
                for (CommandLineParser<?> child : parser.getChildParsers()) {
                    if (child.parsedCommand() != null) {
                        next = child;
                        break;
                    }
                }
            }
            if (next == null)
                break;
            path.add(next);
            parser = next;
        }
        return path;
    }
}
