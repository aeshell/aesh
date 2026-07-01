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

        // Intercept built-in completion flags from args
        if (args != null && args.length > 0) {
            if ("--aesh-complete".equals(args[0]))
                return handleBuiltinComplete(commandRegistry);
            if ("--aesh-completion".equals(args[0]))
                return handleBuiltinCompletion(commandRegistry);
            if ("--aesh-completion-install".equals(args[0]))
                return handleBuiltinCompletionInstall(commandRegistry);
            if ("--aesh-doc".equals(args[0]))
                return handleBuiltinDoc(commandRegistry);
        }

        // Builder API paths (for programmatic use)
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
            return CommandResult.COMMAND_NOT_FOUND;
        } catch (CommandLineParserException | OptionValidatorException e) {
            showHelp(runtime, commandName, args, e);
            return CommandResult.USAGE_ERROR;
        } catch (CommandException | CommandValidatorException e) {
            showHelp(runtime, commandName, args, e);
            return CommandResult.FAILURE;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CommandResult.INTERRUPTED;
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

            // The completion engine already determines the correct candidate set
            // based on parser state — options, subcommands, or argument values.
            // No post-filtering needed (#539).
            java.util.List<org.aesh.terminal.formatting.TerminalString> candidates = completeOperation
                    .getCompletionCandidates();

            // Sort candidates alphabetically for consistent shell completion (#497)
            candidates.sort((a, b) -> a.getCharacters().trim().compareToIgnoreCase(b.getCharacters().trim()));

            for (org.aesh.terminal.formatting.TerminalString candidate : candidates) {
                String value = candidate.getCharacters();
                String desc = descriptions.get(value.trim());
                if (desc != null && !desc.isEmpty()) {
                    System.out.println(value + "\t" + desc);
                } else {
                    System.out.println(value);
                }
            }

            // Emit file/dir sentinel when the cursor is at a position where a
            // positional argument could be typed (not in the middle of an option
            // name/value). Shell scripts merge file completion with other candidates
            // (options, subcommands) from the output above (#539).
            boolean typingOption = partialLine.endsWith("-") || partialLine.endsWith("=")
                    || (partialLine.length() > 0 && !partialLine.endsWith(" ")
                            && partialLine.substring(partialLine.lastIndexOf(' ') + 1).startsWith("-"));
            if (!typingOption) {
                org.aesh.command.option.CompletionFallback fallback = resolvePositionalFallback(
                        commandRegistry, commandName);
                if (fallback == org.aesh.command.option.CompletionFallback.FILES) {
                    System.out.println("__aesh_file__");
                } else if (fallback == org.aesh.command.option.CompletionFallback.DIRECTORIES) {
                    System.out.println("__aesh_dir__");
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

            // Find the deepest group command matching the partial line to scope descriptions.
            // Also collect the path of parsers we walked through so we can add
            // inherited option descriptions from each ancestor (#498).
            java.util.List<CommandLineParser<CommandInvocation>> parserPath = new java.util.ArrayList<>();
            CommandLineParser<CommandInvocation> scopedParser = findScopedParser(parser, args, parserPath);

            // Derive parent name from parser path for variable resolution
            String scopedParentName = parserPath.size() > 1
                    ? parserPath.get(parserPath.size() - 2).getProcessedCommand().name()
                    : null;

            // Add subcommand descriptions scoped to the current group
            if (scopedParser.isGroupCommand()) {
                String scopedName = scopedParser.getProcessedCommand().name();
                for (CommandLineParser<CommandInvocation> child : scopedParser.getAllChildParsers()) {
                    String name = child.getProcessedCommand().name();
                    String desc = org.aesh.command.impl.internal.ProcessedCommand.resolveDescription(
                            child.getProcessedCommand(), child.getProcessedCommand().description(),
                            commandName, scopedName);
                    if (desc != null && !desc.isEmpty()) {
                        descriptions.put(name, desc);
                    }
                }
            }

            // Add option descriptions from the scoped parser (all options)
            addOptionDescriptions(descriptions, scopedParser, commandName, scopedParentName);

            // Add inherited option descriptions from ancestor parsers (#498)
            for (int i = 0; i < parserPath.size(); i++) {
                CommandLineParser<CommandInvocation> ancestor = parserPath.get(i);
                if (ancestor != scopedParser) {
                    String ancestorParent = i > 0 ? parserPath.get(i - 1).getProcessedCommand().name() : null;
                    addOptionDescriptionsFromParser(descriptions, ancestor, true,
                            commandName, ancestorParent);
                }
            }
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
        return findScopedParser(root, args, null);
    }

    /**
     * Walk the parser tree to find the deepest group command matching the args.
     * If parserPath is non-null, collects all parsers along the path (including root).
     * Args may be pre-split (["alias", ""]) or joined (["alias --debug"]),
     * so each arg is split on whitespace before matching.
     */
    @SuppressWarnings("unchecked")
    private static <CI extends CommandInvocation> CommandLineParser<CI> findScopedParser(
            CommandLineParser<CI> root, String[] args, java.util.List<CommandLineParser<CI>> parserPath) {
        if (parserPath != null)
            parserPath.add(root);
        if (args == null || args.length == 0)
            return root;

        // Flatten args into individual tokens (handles both ["alias", ""] and ["alias --debug"])
        java.util.List<String> tokens = new java.util.ArrayList<>();
        for (String arg : args) {
            if (arg == null)
                continue;
            for (String token : arg.split("\\s+")) {
                if (!token.isEmpty())
                    tokens.add(token);
            }
        }

        CommandLineParser<CI> current = root;
        for (String token : tokens) {
            if (token.startsWith("-"))
                break;
            if (!current.isGroupCommand())
                break;
            boolean found = false;
            for (CommandLineParser<CI> child : current.getAllChildParsers()) {
                if (child.getProcessedCommand().name().equals(token)) {
                    current = child;
                    if (parserPath != null)
                        parserPath.add(current);
                    found = true;
                    break;
                }
            }
            if (!found)
                break;
        }
        return current;
    }

    /**
     * Add option descriptions for all options on the given parser.
     * If onlyInherited is true, only adds inherited options (used for parent parsers).
     */
    private static void addOptionDescriptions(java.util.Map<String, String> descriptions,
            CommandLineParser<?> parser, String programName, String parentName) {
        addOptionDescriptionsFromParser(descriptions, parser, false, programName, parentName);
    }

    private static void addOptionDescriptionsFromParser(java.util.Map<String, String> descriptions,
            CommandLineParser<?> parser, boolean onlyInherited,
            String programName, String parentName) {
        org.aesh.command.impl.internal.ProcessedCommand<?, ?> cmd = parser.getProcessedCommand();
        for (org.aesh.command.impl.internal.ProcessedOption opt : cmd.getOptions()) {
            if (onlyInherited && !opt.isInherited())
                continue;
            String desc = org.aesh.command.impl.internal.ProcessedCommand.resolveOptionDesc(
                    cmd, opt, programName, parentName);
            if (desc == null || desc.isEmpty())
                continue;

            // Long name
            String optName = "--" + opt.name();
            descriptions.put(optName, desc);
            // Also with trailing = for value options (completion candidates may include it)
            if (opt.hasValue())
                descriptions.put(optName + "=", desc);

            // Aliases
            for (String alias : opt.getAliases()) {
                descriptions.put("--" + alias, desc);
                if (opt.hasValue())
                    descriptions.put("--" + alias + "=", desc);
            }

            // Negated form
            if (opt.isNegatable() && opt.getNegatedName() != null) {
                descriptions.put("--" + opt.getNegatedName(), desc);
            }

            // Short name (for -v style candidates)
            if (opt.shortName() != null && !opt.shortName().isEmpty()) {
                descriptions.put("-" + opt.shortName(), desc);
            }
        }
    }

    /**
     * Resolve the CompletionFallback for the current positional argument position.
     * Walks the parser tree to find the scoped command, then checks its argument/arguments.
     */
    @SuppressWarnings("unchecked")
    private org.aesh.command.option.CompletionFallback resolvePositionalFallback(
            CommandRegistry commandRegistry, String commandName) {
        try {
            CommandContainer<CommandInvocation> container = (CommandContainer<CommandInvocation>) commandRegistry
                    .getCommand(commandName, "");
            CommandLineParser<CommandInvocation> scopedParser = findScopedParser(container.getParser(), args);
            org.aesh.command.impl.internal.ProcessedCommand<?, ?> cmd = scopedParser.getProcessedCommand();

            // Check @Argument first
            if (cmd.getArgument() != null) {
                return cmd.getArgument().getCompleteFallback();
            }
            // Then @Arguments
            if (cmd.getArguments() != null) {
                return cmd.getArguments().getCompleteFallback();
            }
        } catch (Exception ignored) {
        }
        // Default: offer files for backward compatibility
        return org.aesh.command.option.CompletionFallback.FILES;
    }

    // --- Built-in flag handlers (intercepted from args in execute()) ---

    /**
     * Handles --aesh-complete: runtime callback from shell during tab completion.
     * Extracts partial args after the -- separator and runs the completion engine.
     */
    private CommandResult handleBuiltinComplete(CommandRegistry commandRegistry) {
        // Extract args after the -- separator
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

        this.args = partialArgs;
        this.dynamicComplete = true;
        return performDynamicCompletion(commandRegistry);
    }

    /**
     * Handles --aesh-completion [shell] [--static]: generates a completion script to stdout.
     * <p>
     * Defaults to dynamic completion. Use --static for static scripts.
     * Shell type is auto-detected from $SHELL if not specified.
     * <p>
     * Examples:
     * <ul>
     * <li>{@code --aesh-completion} — dynamic script, auto-detect shell</li>
     * <li>{@code --aesh-completion bash} — dynamic script for bash</li>
     * <li>{@code --aesh-completion --static} — static script, auto-detect shell</li>
     * <li>{@code --aesh-completion --static zsh} — static script for zsh</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private CommandResult handleBuiltinCompletion(CommandRegistry commandRegistry) {
        boolean staticMode = false;
        ShellType shellType = null;

        // Parse remaining args after --aesh-completion
        for (int i = 1; i < args.length; i++) {
            if ("--static".equals(args[i])) {
                staticMode = true;
            } else {
                try {
                    shellType = ShellType.valueOf(args[i].toUpperCase());
                } catch (IllegalArgumentException e) {
                    System.err.println("Unknown shell type: " + args[i]
                            + ". Supported: bash, zsh, fish, pwsh");
                    return CommandResult.FAILURE;
                }
            }
        }

        // Auto-detect shell if not specified
        if (shellType == null) {
            shellType = detectShell();
            if (shellType == null) {
                System.err.println("Could not detect shell type. Specify explicitly: "
                        + "--aesh-completion bash|zsh|fish|pwsh");
                return CommandResult.FAILURE;
            }
        }

        if (staticMode) {
            this.completionShellType = shellType;
            return generateCompletionScript(commandRegistry);
        } else {
            this.dynamicCompletionShellType = shellType;
            return generateDynamicCompletionScript(commandRegistry);
        }
    }

    /**
     * Handles --aesh-completion-install: auto-detect shell, generate dynamic script,
     * and install to the standard completion directory.
     */
    @SuppressWarnings("unchecked")
    private CommandResult handleBuiltinCompletionInstall(CommandRegistry commandRegistry) {
        try {
            String commandName = (String) commandRegistry.getAllCommandNames().iterator().next();
            String name = completionProgramName != null ? completionProgramName : commandName;
            CommandContainer<CommandInvocation> container = (CommandContainer<CommandInvocation>) commandRegistry
                    .getCommand(commandName, "");
            return doInstallCompletion(container.getParser(), name) ? CommandResult.SUCCESS : CommandResult.FAILURE;
        } catch (Exception e) {
            System.err.println("Failed to install completion: " + e.getMessage());
            return CommandResult.FAILURE;
        }
    }

    /**
     * Handles --aesh-doc [asciidoc|markdown]: generates documentation to stdout.
     * <p>
     * Examples:
     * <ul>
     * <li>{@code --aesh-doc} — AsciiDoc to stdout (default)</li>
     * <li>{@code --aesh-doc asciidoc} — AsciiDoc to stdout</li>
     * <li>{@code --aesh-doc markdown} — Markdown to stdout</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private CommandResult handleBuiltinDoc(CommandRegistry commandRegistry) {
        org.aesh.command.DocFormat format = org.aesh.command.DocFormat.ASCIIDOC;

        for (int i = 1; i < args.length; i++) {
            try {
                format = org.aesh.command.DocFormat.valueOf(args[i].toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Unknown doc format: " + args[i]
                        + ". Supported: asciidoc, markdown");
                return CommandResult.FAILURE;
            }
        }

        try {
            String commandName = (String) commandRegistry.getAllCommandNames().iterator().next();
            CommandContainer<CommandInvocation> container = (CommandContainer<CommandInvocation>) commandRegistry
                    .getCommand(commandName, "");

            String programName = completionProgramName != null ? completionProgramName : commandName;
            String doc = org.aesh.util.doc.DocumentationGenerator.builder()
                    .parser(container.getParser())
                    .programName(programName)
                    .format(format)
                    .generateSingle();
            System.out.print(doc);
            return CommandResult.SUCCESS;
        } catch (Exception e) {
            System.err.println("Documentation generation failed: " + e.getMessage());
            return CommandResult.FAILURE;
        }
    }

    // --- Static helper methods (for backwards compatibility) ---

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
    /**
     * Shared logic for completion script installation.
     * Detects shell, generates script, prompts user, and writes file.
     *
     * @return true if installation succeeded or was cancelled, false on error
     */
    private static boolean doInstallCompletion(CommandLineParser<?> parser, String programName) throws Exception {
        ShellType shellType = detectShell();
        if (shellType == null) {
            System.err.println("Could not detect shell type. Use --aesh-completion bash|zsh|fish|pwsh instead.");
            return false;
        }

        java.io.File installPath = getCompletionInstallPath(shellType, programName);
        if (installPath == null) {
            System.err.println("Could not determine completion install path for " + shellType + ".");
            return false;
        }

        String script = ShellCompletionGenerator.forShell(shellType)
                .generateDynamic(parser, programName);

        // Confirm with user
        String action = installPath.exists() ? "Overwrite" : "Write";
        System.out.println(action + " completion script to: " + installPath.getAbsolutePath());
        System.out.print("Proceed? [y/N] ");
        System.out.flush();

        String response = readUserResponse();

        if (response != null && (response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes"))) {
            java.io.File parentDir = installPath.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try (java.io.FileWriter writer = new java.io.FileWriter(installPath)) {
                writer.write(script);
            }
            System.out.println("Completion script installed to " + installPath.getAbsolutePath());
            System.out.println("Note: '" + programName + "' must be on your $PATH for completions to work.");
            System.out.println("Restart your shell or source the file to activate completions.");
        } else {
            System.out.println("Installation cancelled.");
        }
        return true;
    }

    private static String readUserResponse() throws java.io.IOException {
        java.io.Console console = System.console();
        if (console != null) {
            return console.readLine();
        }
        return new java.io.BufferedReader(new java.io.InputStreamReader(System.in)).readLine();
    }

    public static boolean handleCompletionInstall(String[] args, Class<? extends Command> commandClass,
            String programName) {
        if (args == null || args.length == 0 || !"--aesh-completion-install".equals(args[0]))
            return false;

        try {
            CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                    .command(commandClass).create();
            String commandName = registry.getAllCommandNames().iterator().next();
            String name = programName != null ? programName : commandName;
            CommandContainer<CommandInvocation> container = registry.getCommand(commandName, "");
            doInstallCompletion(container.getParser(), name);
        } catch (Exception e) {
            System.err.println("Failed to install completion: " + e.getMessage());
        }
        return true;
    }

    /**
     * Detect the current shell type from environment variables.
     * <p>
     * Checks {@code PSModulePath} first (PowerShell sets this reliably on all
     * platforms), then {@code FISH_VERSION}, {@code ZSH_VERSION},
     * {@code BASH_VERSION}, and falls back to {@code $SHELL}.
     *
     * @return the detected shell type, or null if unknown
     * @since 3.16
     */
    public static ShellType detectShell() {
        // PSModulePath is set by PowerShell on all platforms — check it first.
        // On macOS/Linux, CLI tools are typically launched via a bash wrapper
        // script which sets BASH_VERSION in the environment. If we checked
        // BASH_VERSION first, we'd misdetect PowerShell as bash (#537).
        if (System.getenv("PSModulePath") != null)
            return ShellType.PWSH;

        // Check shell-specific environment variables
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
        if (shell.contains("pwsh") || shell.contains("powershell"))
            return ShellType.PWSH;
        return null;
    }

    /**
     * Detect the current shell type using a custom environment lookup.
     * Package-private — for testing only. Avoids lambda/method-reference
     * allocation on the production path.
     *
     * @param envLookup function that returns the value of an environment variable, or null
     * @return the detected shell type, or null if unknown
     */
    static ShellType detectShell(java.util.function.Function<String, String> envLookup) {
        // PSModulePath is set by PowerShell on all platforms — check it first.
        // On macOS/Linux, CLI tools are typically launched via a bash wrapper
        // script which sets BASH_VERSION in the environment. If we checked
        // BASH_VERSION first, we'd misdetect PowerShell as bash (#537).
        if (envLookup.apply("PSModulePath") != null)
            return ShellType.PWSH;

        // Check shell-specific environment variables
        if (envLookup.apply("FISH_VERSION") != null)
            return ShellType.FISH;
        if (envLookup.apply("ZSH_VERSION") != null)
            return ShellType.ZSH;
        if (envLookup.apply("BASH_VERSION") != null)
            return ShellType.BASH;

        // Fall back to $SHELL
        String shell = envLookup.apply("SHELL");
        if (shell == null || shell.isEmpty()) {
            return null;
        }
        if (shell.contains("fish"))
            return ShellType.FISH;
        if (shell.contains("zsh"))
            return ShellType.ZSH;
        if (shell.contains("bash"))
            return ShellType.BASH;
        if (shell.contains("pwsh") || shell.contains("powershell"))
            return ShellType.PWSH;
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
            case PWSH:
                // ~/.config/powershell/completions/<program>_complete.ps1
                String pwshConfig = System.getenv("XDG_CONFIG_HOME");
                if (pwshConfig == null || pwshConfig.isEmpty())
                    pwshConfig = home + java.io.File.separator + ".config";
                return new java.io.File(
                        pwshConfig + java.io.File.separator + "powershell" + java.io.File.separator + "completions"
                                + java.io.File.separator + programName + "_complete.ps1");
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
