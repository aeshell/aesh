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
package org.aesh.util.completer;

import org.aesh.command.Command;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.container.CommandContainerBuilder;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.CommandLineParserException;

/**
 * Strategy interface for generating shell completion scripts.
 *
 * @author Aesh team
 */
public interface ShellCompletionGenerator {

    /**
     * Generate a static completion script for the given command parser.
     *
     * @param parser the command parser
     * @param programName the name of the program (used in the completion registration)
     * @return the completion script content
     */
    String generate(CommandLineParser<? extends CommandInvocation> parser, String programName);

    /**
     * Generate a static completion script for the given command parser,
     * including only the commands and options accepted by the given filter.
     * <p>
     * The default implementation delegates to {@link #generate(CommandLineParser, String)},
     * ignoring the filter. Built-in generators honor the filter; third-party
     * implementations should override this method to support filtering.
     *
     * @param parser the command parser
     * @param programName the name of the program (used in the completion registration)
     * @param filter decides which commands and options are included
     * @return the completion script content
     */
    default String generate(CommandLineParser<? extends CommandInvocation> parser, String programName,
            CompletionFilter filter) {
        return generate(parser, programName);
    }

    /**
     * Generate a dynamic callback completion script that calls back to the
     * Java process via {@code --aesh-complete} for runtime completions.
     *
     * @param parser the command parser (unused by most implementations, available for future use)
     * @param programName the name of the program
     * @return the dynamic completion script content
     */
    String generateDynamic(CommandLineParser<? extends CommandInvocation> parser, String programName);

    /**
     * Supported shell types.
     */
    enum ShellType {
        BASH("_complete.bash"),
        ZSH("_complete.zsh"),
        FISH(".fish"),
        PWSH("_complete.ps1");

        private final String fileExtension;

        ShellType(String fileExtension) {
            this.fileExtension = fileExtension;
        }

        public String fileExtension() {
            return fileExtension;
        }
    }

    /**
     * Create a generator for the given shell type.
     */
    static ShellCompletionGenerator forShell(ShellType type) {
        switch (type) {
            case BASH:
                return new BashCompletionGenerator();
            case ZSH:
                return new ZshCompletionGenerator();
            case FISH:
                return new FishCompletionGenerator();
            case PWSH:
                return new PowerShellCompletionGenerator();
            default:
                throw new IllegalArgumentException("Unknown shell type: " + type);
        }
    }

    /**
     * One-shot static completion script generation from a command class.
     *
     * @param type shell type
     * @param commandClass the command class annotated with @CommandDefinition
     * @param programName the program name
     * @return the completion script content
     * @throws CommandLineParserException if the command class cannot be parsed
     */
    @SuppressWarnings("unchecked")
    static String generate(ShellType type, Class<? extends Command> commandClass, String programName)
            throws CommandLineParserException {
        return forShell(type).generate(buildParser(commandClass), programName);
    }

    /**
     * One-shot filtered static completion script generation from a command class.
     * <p>
     * Example: exclude a platform-specific command group on builds where it
     * is not registered:
     *
     * <pre>
     * CompletionFilter noVm = new CompletionFilter() {
     *     public boolean includeCommand(ProcessedCommand<?, ?> cmd) {
     *         return !"vm".equals(cmd.name());
     *     }
     *
     *     public boolean includeOption(ProcessedCommand<?, ?> cmd, ProcessedOption opt) {
     *         return true;
     *     }
     * };
     * ShellCompletionGenerator.generate(ShellType.BASH, TopCommand.class, "isx",
     *         CompletionFilter.defaults().and(noVm));
     * </pre>
     *
     * @param type shell type
     * @param commandClass the command class annotated with @CommandDefinition
     * @param programName the program name
     * @param filter decides which commands and options are included
     * @return the completion script content
     * @throws CommandLineParserException if the command class cannot be parsed
     */
    @SuppressWarnings("unchecked")
    static String generate(ShellType type, Class<? extends Command> commandClass, String programName,
            CompletionFilter filter) throws CommandLineParserException {
        return forShell(type).generate(buildParser(commandClass), programName, filter);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static CommandLineParser<CommandInvocation> buildParser(Class<? extends Command> commandClass)
            throws CommandLineParserException {
        CommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        CommandContainer<CommandInvocation> container = builder.create((Class) commandClass);
        return container.getParser();
    }

    /**
     * One-shot dynamic callback completion script generation from a command class.
     *
     * @param type shell type
     * @param commandClass the command class annotated with @CommandDefinition
     * @param programName the program name
     * @return the dynamic completion script content
     * @throws CommandLineParserException if the command class cannot be parsed
     */
    static String generateDynamic(ShellType type, Class<? extends Command> commandClass, String programName)
            throws CommandLineParserException {
        return forShell(type).generateDynamic(buildParser(commandClass), programName);
    }
}
