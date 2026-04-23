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
package org.aesh;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.console.ShellImpl;
import org.aesh.terminal.Connection;
import org.aesh.terminal.tty.TerminalConnection;
import org.aesh.util.completer.ShellCompletionGenerator;
import org.aesh.util.completer.ShellCompletionGenerator.ShellType;

/**
 * @author Aesh team
 */
public class AeshRuntimeRunner {

    private CommandRuntime runtime;

    AeshCommandRegistryBuilder registryBuilder = AeshCommandRegistryBuilder.builder();

    private String[] args;
    private boolean interactive = false;
    private ShellType completionShellType;
    private ShellType dynamicCompletionShellType;
    private String completionProgramName;
    private boolean dynamicComplete = false;

    private AeshRuntimeRunner() {
    }

    public static AeshRuntimeRunner builder() {
        return new AeshRuntimeRunner();
    }

    public AeshRuntimeRunner command(Class<? extends Command> command) {
        try {
            registryBuilder.command(command);
        } catch (CommandRegistryException e) {
            throw new RuntimeException("Exception while building command: " + e.getMessage());
        }
        return this;
    }

    public AeshRuntimeRunner command(Command commandInstance) {
        try {
            registryBuilder.command(commandInstance);
        } catch (CommandRegistryException e) {
            throw new RuntimeException("Exception while building command: " + e.getMessage());
        }
        return this;
    }

    public AeshRuntimeRunner commandRuntime(CommandRuntime runtime) {
        this.runtime = runtime;
        return this;
    }

    public AeshRuntimeRunner interactive(boolean interactive) {
        this.interactive = interactive;
        return this;
    }

    public AeshRuntimeRunner args(String... args) {
        this.args = args;
        return this;
    }

    /**
     * Generate a shell completion script and print it to stdout.
     * When set, {@link #execute()} outputs the script instead of running the command.
     *
     * @param shellType the target shell (BASH, ZSH, or FISH)
     * @return this builder
     */
    public AeshRuntimeRunner generateCompletion(ShellType shellType) {
        this.completionShellType = shellType;
        return this;
    }

    /**
     * Override the program name used in the completion script.
     * Defaults to the command name from @CommandDefinition.
     *
     * @param programName the program name
     * @return this builder
     */
    public AeshRuntimeRunner completionProgramName(String programName) {
        this.completionProgramName = programName;
        return this;
    }

    /**
     * Generate a dynamic callback completion script and print it to stdout.
     * The generated script calls back to the program via {@code --aesh-complete}
     * for runtime completions.
     *
     * @param shellType the target shell (BASH, ZSH, or FISH)
     * @return this builder
     */
    public AeshRuntimeRunner generateDynamicCompletion(ShellType shellType) {
        this.dynamicCompletionShellType = shellType;
        return this;
    }

    /**
     * Enable dynamic completion mode. When set, the runner uses the existing
     * completion engine to produce completions for the partial command line
     * provided via {@link #args(String...)}, printing one candidate per line
     * to stdout.
     *
     * @param dynamicComplete true to enable dynamic completion
     * @return this builder
     */
    public AeshRuntimeRunner dynamicComplete(boolean dynamicComplete) {
        this.dynamicComplete = dynamicComplete;
        return this;
    }

    @SuppressWarnings("unchecked")
    public CommandResult execute() {
        Connection connection = null;
        CommandRegistry commandRegistry = registryBuilder.create();

        if (commandRegistry.getAllCommandNames().size() == 0 && runtime == null)
            throw new RuntimeException("Command needs to be added");

        if (completionShellType != null) {
            return generateCompletionScript(commandRegistry);
        }
        if (dynamicCompletionShellType != null) {
            return generateDynamicCompletionScript(commandRegistry);
        }
        if (dynamicComplete) {
            return performDynamicCompletion(commandRegistry);
        }
        try {

            if (runtime == null) {
                AeshCommandRuntimeBuilder runtimeBuilder = AeshCommandRuntimeBuilder.builder();
                if (interactive) {
                    connection = new TerminalConnection();
                    connection.openNonBlocking();
                    runtimeBuilder.shell(new ShellImpl(connection));
                }

                runtime = runtimeBuilder.commandRegistry(commandRegistry).build();

            }

            final Set<String> commandNames = runtime.getCommandRegistry().getAllCommandNames();
            if (commandNames.isEmpty())
                throw new RuntimeException("Command needs to be added to the registry.");
            else if (commandNames.size() > 1)
                throw new RuntimeException("Only one command can be added to the registry.");

            final String commandName = commandNames.iterator().next();

            CommandResult result = null;
            try {
                result = runtime.executeCommand(commandName, args);
            } catch (CommandNotFoundException e) {
                System.err.println("Command not found: " + commandName);
            } catch (CommandException | CommandLineParserException | CommandValidatorException | OptionValidatorException e) {
                showHelpIfNeeded(runtime, commandName, args, e);
            } catch (InterruptedException | IOException e) {
                System.err.println(e.getMessage());
            }
            if (connection != null)
                connection.close();

            return result;
        } catch (IOException e) {
            throw new RuntimeException("Exception while executing command: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private CommandResult generateCompletionScript(CommandRegistry commandRegistry) {
        try {
            String commandName = (String) commandRegistry.getAllCommandNames().iterator().next();
            CommandContainer<CommandInvocation> container = (CommandContainer<CommandInvocation>) commandRegistry
                    .getCommand(commandName, "");

            String programName = completionProgramName != null
                    ? completionProgramName
                    : commandName;

            ShellCompletionGenerator generator = ShellCompletionGenerator.forShell(completionShellType);
            String script = generator.generate(container.getParser(), programName);
            System.out.print(script);
            return CommandResult.SUCCESS;
        } catch (CommandNotFoundException e) {
            System.err.println("Command not found: " + e.getMessage());
            return CommandResult.FAILURE;
        }
    }

    @SuppressWarnings("unchecked")
    private CommandResult generateDynamicCompletionScript(CommandRegistry commandRegistry) {
        try {
            String commandName = (String) commandRegistry.getAllCommandNames().iterator().next();
            CommandContainer<CommandInvocation> container = (CommandContainer<CommandInvocation>) commandRegistry
                    .getCommand(commandName, "");

            String programName = completionProgramName != null
                    ? completionProgramName
                    : commandName;

            ShellCompletionGenerator generator = ShellCompletionGenerator.forShell(dynamicCompletionShellType);
            String script = generator.generateDynamic(container.getParser(), programName);
            System.out.print(script);
            return CommandResult.SUCCESS;
        } catch (CommandNotFoundException e) {
            System.err.println("Command not found: " + e.getMessage());
            return CommandResult.FAILURE;
        }
    }

    @SuppressWarnings("unchecked")
    private CommandResult performDynamicCompletion(CommandRegistry commandRegistry) {
        try {
            CommandRuntime<CommandInvocation> rt = AeshCommandRuntimeBuilder.builder()
                    .commandRegistry(commandRegistry)
                    .build();

            // The completion engine expects the command name as the first word.
            // The shell passes args after the program name, so we prepend the
            // registered command name.
            String commandName = (String) commandRegistry.getAllCommandNames().iterator().next();
            String partialLine = args != null ? String.join(" ", args) : "";
            String buffer = partialLine.isEmpty() ? commandName + " " : commandName + " " + partialLine;

            AeshCompleteOperation completeOperation = new AeshCompleteOperation(buffer, buffer.length());
            rt.complete(completeOperation);

            for (org.aesh.terminal.formatting.TerminalString candidate : completeOperation.getCompletionCandidates()) {
                System.out.println(candidate.getCharacters());
            }
            return CommandResult.SUCCESS;
        } catch (Exception e) {
            System.err.println("Completion error: " + e.getMessage());
            return CommandResult.FAILURE;
        }
    }

    /**
     * Check if args contain {@code --aesh-complete} and handle dynamic completion.
     * This is a convenience method for use in {@code main()} methods.
     *
     * @param args the command-line arguments
     * @param commandClass the command class
     * @return true if completion was handled, false if normal execution should continue
     */
    public static boolean handleDynamicCompletion(String[] args, Class<? extends Command> commandClass) {
        if (args == null || args.length == 0 || !"--aesh-complete".equals(args[0])) {
            return false;
        }

        // Find the -- separator
        int separatorIndex = -1;
        for (int i = 1; i < args.length; i++) {
            if ("--".equals(args[i])) {
                separatorIndex = i;
                break;
            }
        }

        String[] partialArgs;
        if (separatorIndex >= 0 && separatorIndex < args.length - 1) {
            partialArgs = Arrays.copyOfRange(args, separatorIndex + 1, args.length);
        } else {
            partialArgs = new String[0];
        }

        AeshRuntimeRunner.builder()
                .command(commandClass)
                .dynamicComplete(true)
                .args(partialArgs)
                .execute();

        return true;
    }

    private static void showHelpIfNeeded(CommandRuntime runtime, String commandName, String[] args, Exception e) {
        if (e != null) {
            System.err.println(e.getMessage());
        }
        // Build a lookup line that includes the subcommand name so commandInfo
        // resolves help for the correct subcommand, not the root command.
        String helpLine = buildHelpLine(runtime, commandName, args);
        System.err.println(runtime.commandInfo(helpLine));
    }

    private static String buildHelpLine(CommandRuntime runtime, String commandName, String[] args) {
        if (args == null || args.length == 0) {
            return commandName;
        }
        try {
            @SuppressWarnings("unchecked")
            CommandContainer<CommandInvocation> container = (CommandContainer<CommandInvocation>) runtime.getCommandRegistry()
                    .getCommand(commandName, "");
            if (container.getParser().isGroupCommand()) {
                StringBuilder line = new StringBuilder(commandName);
                CommandLineParser<?> current = container.getParser();
                for (String arg : args) {
                    if (arg.startsWith("-"))
                        continue;
                    CommandLineParser<?> child = current.getChildParser(arg);
                    if (child != null) {
                        line.append(' ').append(arg);
                        if (child.isGroupCommand()) {
                            current = child;
                        } else {
                            return line.toString();
                        }
                    }
                }
                if (line.length() > commandName.length())
                    return line.toString();
            }
        } catch (CommandNotFoundException ex) {
            // fall through
        }
        return commandName;
    }
}
