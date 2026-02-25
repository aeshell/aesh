package org.aesh.command.impl.completer;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aesh.command.CommandNotFoundException;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.readline.SuggestionProvider;
import org.aesh.terminal.utils.LoggerUtil;

/**
 * Suggests command names, subcommand names, and option names from a {@link CommandRegistry}.
 * <p>
 * Only suggests when there is exactly one unambiguous match.
 */
public class CommandSuggestionProvider<CI extends CommandInvocation> implements SuggestionProvider {

    private static final Logger LOGGER = LoggerUtil.getLogger(CommandSuggestionProvider.class.getName());

    private final CommandRegistry<CI> registry;

    public CommandSuggestionProvider(CommandRegistry<CI> registry) {
        this.registry = registry;
    }

    @Override
    public String suggest(String buffer) {
        if (buffer == null || buffer.isEmpty()) {
            return null;
        }

        String trimmed = buffer.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        // Check if the user is typing an option (--something)
        int lastSpace = buffer.lastIndexOf(' ');
        if (lastSpace >= 0) {
            String lastWord = buffer.substring(lastSpace + 1);
            if (lastWord.startsWith("--")) {
                return suggestOption(trimmed, lastWord);
            }
        }

        // Check if buffer contains spaces -> could be subcommand
        if (trimmed.contains(" ")) {
            return suggestSubcommand(trimmed);
        }

        // Single word -> suggest command name
        return suggestCommand(trimmed, buffer);
    }

    private String suggestCommand(String prefix, String originalBuffer) {
        Set<String> allNames = registry.getAllCommandNames();
        String match = null;
        for (String name : allNames) {
            if (name.startsWith(prefix) && !name.equals(prefix)) {
                if (!isCommandActivated(name)) {
                    continue;
                }
                if (match != null) {
                    // Ambiguous - more than one match
                    return null;
                }
                match = name;
            }
        }
        if (match != null) {
            return match.substring(prefix.length());
        }
        return null;
    }

    private String suggestSubcommand(String trimmed) {
        // Split into command name and the rest
        int firstSpace = trimmed.indexOf(' ');
        String commandName = trimmed.substring(0, firstSpace);
        String rest = trimmed.substring(firstSpace + 1).trim();

        if (rest.isEmpty()) {
            return null;
        }

        try {
            CommandContainer<CI> container = registry.getCommand(commandName, trimmed);
            CommandLineParser<CI> parser = container.getParser();

            if (parser.isGroupCommand()) {
                // rest is a partial subcommand name
                List<CommandLineParser<CI>> childParsers = parser.getAllChildParsers();
                String match = null;
                for (CommandLineParser<CI> child : childParsers) {
                    String childName = child.getProcessedCommand().name();
                    if (childName.startsWith(rest) && !childName.equals(rest)) {
                        if (match != null) {
                            return null; // ambiguous
                        }
                        match = childName;
                    }
                }
                if (match != null) {
                    return match.substring(rest.length());
                }
            }
        } catch (CommandNotFoundException e) {
            // Command not found, no suggestion
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error suggesting subcommand", e);
        }
        return null;
    }

    private String suggestOption(String trimmed, String lastWord) {
        // Extract command name from the full buffer
        String[] parts = trimmed.split("\\s+");
        if (parts.length < 1) {
            return null;
        }

        String commandName = parts[0];
        String prefix = lastWord.substring(2); // strip --

        try {
            CommandContainer<CI> container = registry.getCommand(commandName, trimmed);
            CommandLineParser<CI> parser = container.getParser();

            ProcessedCommand<?, ?> processedCommand;

            // For group commands, check if we have a subcommand
            if (parser.isGroupCommand() && parts.length >= 2 && !parts[1].startsWith("-")) {
                CommandLineParser<CI> childParser = parser.getChildParser(parts[1]);
                if (childParser != null) {
                    processedCommand = childParser.getProcessedCommand();
                } else {
                    processedCommand = parser.getProcessedCommand();
                }
            } else {
                processedCommand = parser.getProcessedCommand();
            }

            List<String> possibleNames = processedCommand.findPossibleLongNames(prefix);
            if (possibleNames.size() == 1) {
                String optionName = possibleNames.get(0);
                String suffix = optionName.substring(prefix.length());
                // Append = if the option takes a value
                ProcessedOption option = processedCommand.findLongOptionNoActivatorCheck(optionName);
                if (option != null && option.hasValue()) {
                    suffix += "=";
                }
                return suffix;
            }
        } catch (CommandNotFoundException e) {
            // Command not found, no suggestion
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error suggesting option", e);
        }
        return null;
    }

    private boolean isCommandActivated(String name) {
        try {
            CommandContainer<CI> container = registry.getCommand(name, name);
            return container.getParser().getProcessedCommand().getActivator().isActivated(null);
        } catch (Exception e) {
            return true; // default to activated if we can't check
        }
    }
}
