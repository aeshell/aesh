/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Thrown when a group command receives an unrecognized subcommand name.
 * Carries the unknown subcommand, the parent command name, and the
 * available subcommand names for "did you mean?" suggestions.
 * <p>
 * Extends {@link CommandLineParserException} so existing catch blocks
 * that handle parser errors continue to work unchanged.
 */
public class SubcommandNotFoundException extends CommandLineParserException {

    private final String parentCommand;
    private final String unknownSubcommand;
    private final Collection<String> availableSubcommands;
    private final List<String> suggestions;

    public SubcommandNotFoundException(String parentCommand, String unknownSubcommand,
            Collection<String> availableSubcommands) {
        this(parentCommand, unknownSubcommand, availableSubcommands,
                CommandSuggestions.suggest(unknownSubcommand, availableSubcommands));
    }

    /**
     * Create the exception with explicit suggestions, primarily for testing.
     * Production throw sites use the three-argument constructor, which
     * derives suggestions from the available names.
     */
    public SubcommandNotFoundException(String parentCommand, String unknownSubcommand,
            Collection<String> availableSubcommands, List<String> suggestions) {
        super(buildMessage(parentCommand, unknownSubcommand, availableSubcommands, suggestions));
        this.parentCommand = parentCommand;
        this.unknownSubcommand = unknownSubcommand;
        this.availableSubcommands = availableSubcommands != null
                ? Collections.unmodifiableCollection(availableSubcommands)
                : Collections.emptyList();
        this.suggestions = suggestions != null
                ? Collections.unmodifiableList(new ArrayList<>(suggestions))
                : Collections.emptyList();
    }

    public String getParentCommand() {
        return parentCommand;
    }

    public String getUnknownSubcommand() {
        return unknownSubcommand;
    }

    public Collection<String> getAvailableSubcommands() {
        return availableSubcommands;
    }

    /**
     * Close matches for the unknown subcommand, best first, or empty when
     * nothing resembles it. Computed from the available names with
     * {@link CommandSuggestions}.
     */
    public List<String> getSuggestions() {
        return suggestions;
    }

    private static String buildMessage(String parentCommand, String unknownSubcommand,
            Collection<String> availableSubcommands, List<String> suggestions) {
        StringBuilder sb = new StringBuilder();
        sb.append("'").append(parentCommand).append(" ").append(unknownSubcommand)
                .append("' is not a subcommand of '").append(parentCommand).append("'.");
        if (suggestions != null && !suggestions.isEmpty()) {
            sb.append(" Did you mean: ").append(joinWithOr(suggestions)).append("?");
        } else if (availableSubcommands != null && !availableSubcommands.isEmpty()) {
            sb.append(" Available: ").append(String.join(", ", availableSubcommands)).append(".");
        }
        sb.append(" See '").append(parentCommand).append(" --help'.");
        return sb.toString();
    }

    private static String joinWithOr(List<String> suggestions) {
        if (suggestions.size() == 1)
            return suggestions.get(0);
        return String.join(", ", suggestions.subList(0, suggestions.size() - 1))
                + " or " + suggestions.get(suggestions.size() - 1);
    }
}
