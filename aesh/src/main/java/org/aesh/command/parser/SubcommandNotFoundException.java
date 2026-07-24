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

import java.util.Collection;
import java.util.Collections;

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

    public SubcommandNotFoundException(String parentCommand, String unknownSubcommand,
            Collection<String> availableSubcommands) {
        super(buildMessage(parentCommand, unknownSubcommand, availableSubcommands));
        this.parentCommand = parentCommand;
        this.unknownSubcommand = unknownSubcommand;
        this.availableSubcommands = availableSubcommands != null
                ? Collections.unmodifiableCollection(availableSubcommands)
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

    private static String buildMessage(String parentCommand, String unknownSubcommand,
            Collection<String> availableSubcommands) {
        StringBuilder sb = new StringBuilder();
        sb.append("'").append(parentCommand).append(" ").append(unknownSubcommand)
                .append("' is not a subcommand of '").append(parentCommand).append("'.");
        if (availableSubcommands != null && !availableSubcommands.isEmpty()) {
            sb.append(" Available: ").append(String.join(", ", availableSubcommands)).append(".");
        }
        sb.append(" See '").append(parentCommand).append(" --help'.");
        return sb.toString();
    }
}
