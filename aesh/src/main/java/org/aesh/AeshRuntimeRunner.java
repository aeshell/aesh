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
import org.aesh.command.DefaultValueProvider;
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

    /**
     * Set a custom CommandContainerBuilder for command creation.
     * This allows frameworks (e.g., CDI, Spring) to inject dependencies
     * into subcommands and option service providers.
     */
    public AeshRuntimeRunner containerBuilder(org.aesh.command.container.CommandContainerBuilder<?> containerBuilder) {
        this.registryBuilder.containerBuilder(containerBuilder);
        return this;
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

    /**
     * Set a registry-level DefaultValueProvider that applies to all commands
     * that don't declare their own per-command provider via the annotation.
     */
    public AeshRuntimeRunner defaultValueProvider(DefaultValueProvider provider) {
        this.registryBuilder.defaultValueProvider(provider);
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

            // For dynamic shell completion, filter out option candidates when the
            // cursor is at a positional argument position (no - prefix typed).
            // This lets the shell provide its default file completion instead.
            boolean cursorAtPositional = partialLine.isEmpty()
                    || (!partialLine.endsWith("-") && (partialLine.endsWith(" ") || !partialLine.contains("-")));
            java.util.List<org.aesh.terminal.formatting.TerminalString> candidates = completeOperation
                    .getCompletionCandidates();
            if (cursorAtPositional) {
                candidates.removeIf(c -> c.getCharacters().trim().startsWith("-"));
            }

            for (org.aesh.terminal.formatting.TerminalString candidate : candidates) {
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
            CommandContainer<CommandInvocation> container = commandRegistry.getCommand(commandName, "");
            CommandLineParser<CommandInvocation> parser = container.getParser();

            // Find the deepest group command matching the partial line to scope descriptions
            CommandLineParser<CommandInvocation> scopedParser = findScopedParser(parser, args);

            // Add subcommand descriptions scoped to the current group
            if (scopedParser.isGroupCommand()) {
                for (CommandLineParser<CommandInvocation> child : scopedParser.getAllChildParsers()) {
                    String name = child.getProcessedCommand().name();
                    String desc = child.getProcessedCommand().description();
                    if (desc != null && !desc.isEmpty()) {
                        descriptions.put(name, desc);
                    }
                }
            }

            // Add option descriptions from the scoped parser
            addOptionDescriptions(descriptions, scopedParser);
        } catch (Exception ignored) {
        }
        return descriptions;
    }

    /**
     * Walk the parser tree to find the deepest group command matching the args.
     * For example, with args ["alias", ""] and a parser tree jbang > alias > {add, list, remove},
     * this returns the "alias" parser so descriptions come from alias's children only.
     */
    @SuppressWarnings("unchecked")
    private static <CI extends CommandInvocation> CommandLineParser<CI> findScopedParser(
            CommandLineParser<CI> root, String[] args) {
        if (args == null || args.length == 0)
            return root;
        CommandLineParser<CI> current = root;
        for (String arg : args) {
            if (arg == null || arg.isEmpty() || arg.startsWith("-"))
                break;
            if (!current.isGroupCommand())
                break;
            boolean found = false;
            for (CommandLineParser<CI> child : current.getAllChildParsers()) {
                if (child.getProcessedCommand().name().equals(arg)) {
                    current = child;
                    found = true;
                    break;
                }
            }
            if (!found)
                break;
        }
        return current;
    }

    private static void addOptionDescriptions(java.util.Map<String, String> descriptions,
            CommandLineParser<?> parser) {
        for (org.aesh.command.impl.internal.ProcessedOption opt : parser.getProcessedCommand().getOptions()) {
            String optName = "--" + opt.name();
            if (opt.description() != null && !opt.description().isEmpty()) {
                descriptions.put(optName, opt.description());
            }
            // Add description for negated form of negatable options
            if (opt.isNegatable() && opt.getNegatedName() != null) {
                String negatedName = "--" + opt.getNegatedName();
                if (opt.description() != null && !opt.description().isEmpty()) {
                    descriptions.put(negatedName, opt.description());
                }
            }
        }
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

    /**
     * Handles the --aesh-completion-install flag. Detects the user's shell,
     * generates a dynamic completion script, and installs it to the appropriate
     * location after user confirmation.
     *
     * @param args the command-line arguments
     * @param commandClass the command class to generate completions for
     * @return true if the flag was handled, false if not a completion install request
     */
    public static boolean handleCompletionInstall(String[] args, Class<? extends Command> commandClass) {
        return handleCompletionInstall(args, commandClass, null);
    }

    /**
     * Handles the --aesh-completion-install flag with a custom program name.
     *
     * @param args the command-line arguments
     * @param commandClass the command class to generate completions for
     * @param programName the program name to use in the completion script (null = use command name)
     * @return true if the flag was handled, false if not a completion install request
     */
    @SuppressWarnings("unchecked")
    public static boolean handleCompletionInstall(String[] args, Class<? extends Command> commandClass,
            String programName) {
        if (args == null || args.length == 0 || !"--aesh-completion-install".equals(args[0]))
            return false;

        try {
            CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                    .command(commandClass).create();
            String commandName = registry.getAllCommandNames().iterator().next();
            String name = programName != null ? programName : commandName;

            ShellType shellType = detectShell();
            if (shellType == null) {
                System.err.println("Could not detect shell type. Set $SHELL or use --generate-completion instead.");
                return true;
            }

            java.io.File installPath = getCompletionInstallPath(shellType, name);
            if (installPath == null) {
                System.err.println("Could not determine completion install path for " + shellType + ".");
                return true;
            }

            // Generate the script
            CommandContainer<CommandInvocation> container = registry.getCommand(commandName, "");
            String script = ShellCompletionGenerator.forShell(shellType)
                    .generateDynamic(container.getParser(), name);

            // Confirm with user
            String action = installPath.exists() ? "Overwrite" : "Write";
            System.out.println(action + " completion script to: " + installPath.getAbsolutePath());
            System.out.print("Proceed? [y/N] ");
            System.out.flush();

            java.io.Console console = System.console();
            String response;
            if (console != null) {
                response = console.readLine();
            } else {
                // Fallback for non-interactive (piped input)
                response = new java.io.BufferedReader(new java.io.InputStreamReader(System.in)).readLine();
            }

            if (response != null && (response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes"))) {
                java.io.File parentDir = installPath.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                try (java.io.FileWriter writer = new java.io.FileWriter(installPath)) {
                    writer.write(script);
                }
                System.out.println("Completion script installed to " + installPath.getAbsolutePath());
                System.out.println("Note: '" + name + "' must be on your $PATH for completions to work.");
                System.out.println("Restart your shell or source the file to activate completions.");
            } else {
                System.out.println("Installation cancelled.");
            }
        } catch (Exception e) {
            System.err.println("Failed to install completion: " + e.getMessage());
        }
        return true;
    }

    static ShellType detectShell() {
        // Check shell-specific environment variables first
        if (System.getenv("FISH_VERSION") != null)
            return ShellType.FISH;
        if (System.getenv("ZSH_VERSION") != null)
            return ShellType.ZSH;
        if (System.getenv("BASH_VERSION") != null)
            return ShellType.BASH;

        // Fall back to $SHELL
        String shell = System.getenv("SHELL");
        if (shell == null || shell.isEmpty())
            return null;
        if (shell.contains("fish"))
            return ShellType.FISH;
        if (shell.contains("zsh"))
            return ShellType.ZSH;
        if (shell.contains("bash"))
            return ShellType.BASH;
        return null;
    }

    static java.io.File getCompletionInstallPath(ShellType shellType, String programName) {
        String home = System.getProperty("user.home");
        if (home == null)
            return null;

        switch (shellType) {
            case BASH:
                // ~/.bash_completion.d/<program>
                return new java.io.File(home, ".bash_completion.d" + java.io.File.separator + programName);
            case ZSH:
                // ~/.zsh/completions/_<program>
                return new java.io.File(home,
                        ".zsh" + java.io.File.separator + "completions" + java.io.File.separator + "_" + programName);
            case FISH:
                // ~/.config/fish/completions/<program>.fish
                String configDir = System.getenv("XDG_CONFIG_HOME");
                if (configDir == null || configDir.isEmpty())
                    configDir = home + java.io.File.separator + ".config";
                return new java.io.File(
                        configDir + java.io.File.separator + "fish" + java.io.File.separator + "completions"
                                + java.io.File.separator + programName + ".fish");
            default:
                return null;
        }
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
