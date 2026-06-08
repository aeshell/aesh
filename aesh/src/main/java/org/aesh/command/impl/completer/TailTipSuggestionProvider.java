/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command.impl.completer;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.aesh.command.CommandNotFoundException;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.readline.suggestion.SuggestionProvider;
import org.aesh.terminal.utils.LoggerUtil;

/**
 * Provides tail tip suggestions — dimmed parameter hints showing
 * remaining options and arguments after the cursor.
 * <p>
 * Only activates when the buffer ends with a space (the user has finished
 * typing a word and the command is identified). When the user is mid-word,
 * returns null to let other suggestion providers (auto-suggest, history)
 * handle the suggestion.
 * <p>
 * This provider parses the current buffer in COMPLETION mode to determine
 * which options have already been provided, then builds a synopsis of the
 * remaining parameters.
 *
 * @author Aesh team
 */
public class TailTipSuggestionProvider<CI extends CommandInvocation> implements SuggestionProvider {

    private static final Logger LOGGER = LoggerUtil.getLogger(TailTipSuggestionProvider.class.getName());

    private final CommandRegistry<CI> registry;

    // Cache to avoid re-parsing when the buffer hasn't changed
    private String lastBuffer;
    private String lastTip;

    public TailTipSuggestionProvider(CommandRegistry<CI> registry) {
        this.registry = registry;
    }

    @Override
    public String suggest(String buffer) {
        if (buffer == null || buffer.isEmpty()) {
            return null;
        }

        // Only show tail tips when the cursor is after a space (not mid-word).
        // Mid-word suggestions are handled by CommandSuggestionProvider.
        if (!buffer.endsWith(" ")) {
            lastBuffer = null;
            lastTip = null;
            return null;
        }

        // Cache hit — same buffer as last call
        if (buffer.equals(lastBuffer)) {
            return lastTip;
        }

        String trimmed = buffer.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        // Must contain at least a command name
        String[] parts = trimmed.split("\\s+");
        if (parts.length < 1) {
            return null;
        }

        String commandName = parts[0];

        try {
            CommandContainer<CI> container = registry.getCommand(commandName, trimmed);
            CommandLineParser<CI> parser = container.getParser();

            // For group commands, resolve to the subcommand parser if present
            CommandLineParser<CI> targetParser = parser;
            if (parser.isGroupCommand() && parts.length >= 2 && !parts[1].startsWith("-")) {
                CommandLineParser<CI> childParser = parser.getChildParser(parts[1]);
                if (childParser != null) {
                    targetParser = childParser;
                } else {
                    // Partial subcommand name or unknown — don't show tail tip
                    lastBuffer = buffer;
                    lastTip = null;
                    return null;
                }
            }

            // Parse the buffer in COMPLETION mode to populate option values.
            // This tells us which options have already been specified.
            try {
                targetParser.parse(trimmed, CommandLineParser.Mode.COMPLETION);

                // Build the remaining synopsis (options + args not yet provided)
                String tip = targetParser.getProcessedCommand()
                        .buildRemainingSynopsis(targetParser.isGroupCommand());

                // Clean up parser state
                targetParser.clear();

                if (tip != null && !tip.trim().isEmpty()) {
                    lastBuffer = buffer;
                    lastTip = tip;
                    return tip;
                }
            } catch (Exception e) {
                // Parse errors are expected for partial/invalid input
                LOGGER.log(Level.FINEST, "Tail tip parse error", e);
                try {
                    targetParser.clear();
                } catch (Exception ignored) {
                }
            }
        } catch (CommandNotFoundException e) {
            // Command not recognized yet — no tail tip
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error generating tail tip", e);
        }

        lastBuffer = buffer;
        lastTip = null;
        return null;
    }
}
