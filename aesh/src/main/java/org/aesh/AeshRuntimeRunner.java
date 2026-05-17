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
import org.aesh.util.completer.ShellCompletionGenerator;
import org.aesh.util.completer.ShellCompletionGenerator.ShellType;

/**
 * Non-interactive single-command runner for CLI tools.
 * For interactive shells, use {@link AeshConsoleRunner}.
 */
public class AeshRuntimeRunner {

    private final AeshCommandRegistryBuilder registryBuilder = AeshCommandRegistryBuilder.builder();
    private String[] args;
    private ShellType completionShellType;
    private ShellType dynamicCompletionShellType;
    private String completionProgramName;
    private boolean dynamicComplete;

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

    public AeshRuntimeRunner args(String... args) {
        this.args = args;
        return this;
    }

    public AeshRuntimeRunner generateCompletion(ShellType shellType) {
        this.completionShellType = shellType;
        return this;
    }

    public AeshRuntimeRunner completionProgramName(String programName) {
        this.completionProgramName = programName;
        return this;
    }

    public AeshRuntimeRunner generateDynamicCompletion(ShellType shellType) {
        this.dynamicCompletionShellType = shellType;
        return this;
    }

    public AeshRuntimeRunner dynamicComplete(boolean dynamicComplete) {
        this.dynamicComplete = dynamicComplete;
        return this;
    }

    @SuppressWarnings("unchecked")
    public CommandResult execute() {
        CommandRegistry commandRegistry = registryBuilder.create();

        if (commandRegistry.getAllCommandNames().isEmpty())
            throw new RuntimeException("Command needs to be added");

        if (completionShellType != null)
            return generateCompletionScript(commandRegistry);
        if (dynamicCompletionShellType != null)
            return generateDynamicCompletionScript(commandRegistry);
        if (dynamicComplete)
            return performDynamicCompletion(commandRegistry);

        CommandRuntime runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(commandRegistry).build();

        String commandName = (String) commandRegistry.getAllCommandNames().iterator().next();

        try {
            return runtime.executeCommand(commandName, args);
        } catch (CommandNotFoundException e) {
            System.err.println("Command not found: " + commandName);
            return CommandResult.FAILURE;
        } catch (CommandLineParserException | OptionValidatorException e) {
            showHelp(runtime, commandName, args, e);
            return CommandResult.valueOf(2);
        } catch (CommandException | CommandValidatorException e) {
            showHelp(runtime, commandName, args, e);
            return CommandResult.FAILURE;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CommandResult.FAILURE;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return CommandResult.FAILURE;
        }
    }

    @SuppressWarnings("unchecked")
    private CommandResult generateCompletionScript(CommandRegistry commandRegistry) {
        try {
            String commandName = (String) commandRegistry.getAllCommandNames().iterator().next();
            CommandContainer<CommandInvocation> container = (CommandContainer<CommandInvocation>) commandRegistry
                    .getCommand(commandName, "");

            String programName = completionProgramName != null ? completionProgramName : commandName;
            String script = ShellCompletionGenerator.forShell(completionShellType)
                    .generate(container.getParser(), programName);
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

            String programName = completionProgramName != null ? completionProgramName : commandName;
            String script = ShellCompletionGenerator.forShell(dynamicCompletionShellType)
                    .generateDynamic(container.getParser(), programName);
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
                    .commandRegistry(commandRegistry).build();

            String commandName = (String) commandRegistry.getAllCommandNames().iterator().next();
            String partialLine = args != null ? String.join(" ", args) : "";
            String buffer = partialLine.isEmpty() ? commandName + " " : commandName + " " + partialLine;

            AeshCompleteOperation completeOperation = new AeshCompleteOperation(buffer, buffer.length());
            rt.complete(completeOperation);

            // Build a map of subcommand/option names to descriptions for richer output
            java.util.Map<String, String> descriptions = buildCompletionDescriptions(commandRegistry, commandName);

            for (org.aesh.terminal.formatting.TerminalString candidate : completeOperation.getCompletionCandidates()) {
                String value = candidate.getCharacters();
                String desc = descriptions.get(value.trim());
                if (desc != null && !desc.isEmpty()) {
                    System.out.println(value + "\t" + desc);
                } else {
                    System.out.println(value);
                }
            }
            return CommandResult.SUCCESS;
        } catch (Exception e) {
            System.err.println("Completion error: " + e.getMessage());
            return CommandResult.FAILURE;
        }
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, String> buildCompletionDescriptions(CommandRegistry commandRegistry,
            String commandName) {
        java.util.Map<String, String> descriptions = new java.util.HashMap<>();
        try {
            // Add subcommand descriptions
            java.util.List<CommandLineParser<CommandInvocation>> children = commandRegistry
                    .getChildCommandParsers(commandName);
            if (children != null) {
                for (CommandLineParser<CommandInvocation> child : children) {
                    String name = child.getProcessedCommand().name();
                    String desc = child.getProcessedCommand().description();
                    if (desc != null && !desc.isEmpty()) {
                        descriptions.put(name, desc);
                    }
                }
            }
        } catch (Exception ignored) {
            // Not a group command or no children -- that's fine
        }
        try {
            // Add option descriptions from the main command
            CommandContainer<CommandInvocation> container = commandRegistry.getCommand(commandName, "");
            CommandLineParser<CommandInvocation> parser = container.getParser();
            for (org.aesh.command.impl.internal.ProcessedOption opt : parser.getProcessedCommand().getOptions()) {
                String optName = "--" + opt.name();
                if (opt.description() != null && !opt.description().isEmpty()) {
                    descriptions.put(optName, opt.description());
                }
            }
        } catch (Exception ignored) {
        }
        return descriptions;
    }

    public static boolean handleDynamicCompletion(String[] args, Class<? extends Command> commandClass) {
        if (args == null || args.length == 0 || !"--aesh-complete".equals(args[0]))
            return false;

        int separatorIndex = -1;
        for (int i = 1; i < args.length; i++) {
            if ("--".equals(args[i])) {
                separatorIndex = i;
                break;
            }
        }

        String[] partialArgs = separatorIndex >= 0 && separatorIndex < args.length - 1
                ? Arrays.copyOfRange(args, separatorIndex + 1, args.length)
                : new String[0];

        AeshRuntimeRunner.builder()
                .command(commandClass)
                .dynamicComplete(true)
                .args(partialArgs)
                .execute();

        return true;
    }

    private static void showHelp(CommandRuntime runtime, String commandName, String[] args, Exception e) {
        System.err.println(e.getMessage());
        String helpLine = resolveHelpCommand(runtime, commandName, args);
        System.err.println(runtime.commandInfo(helpLine));
    }

    @SuppressWarnings("unchecked")
    private static String resolveHelpCommand(CommandRuntime runtime, String commandName, String[] args) {
        if (args == null || args.length == 0)
            return commandName;
        try {
            CommandContainer<CommandInvocation> container = (CommandContainer<CommandInvocation>) runtime
                    .getCommandRegistry().getCommand(commandName, "");
            if (container.getParser().isGroupCommand()) {
                StringBuilder line = new StringBuilder(commandName);
                CommandLineParser<?> current = container.getParser();
                for (String arg : args) {
                    if (arg.startsWith("-"))
                        continue;
                    CommandLineParser<?> child = current.getChildParser(arg);
                    if (child != null) {
                        line.append(' ').append(arg);
                        if (child.isGroupCommand())
                            current = child;
                        else
                            return line.toString();
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
