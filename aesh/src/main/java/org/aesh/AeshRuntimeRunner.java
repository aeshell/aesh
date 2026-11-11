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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.ArgumentsSection;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinitionException;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandNotFoundHandler;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.DefaultValueProvider;
import org.aesh.command.HelpFeature;
import org.aesh.command.Toggle;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.container.CommandContainerBuilder;
import org.aesh.command.impl.LazyHelp;
import org.aesh.command.impl.LazyRouteResolver;
import org.aesh.command.impl.completer.DefaultValueOptionCompleter;
import org.aesh.command.impl.completer.FileOptionCompleter;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.CompletionFallback;
import org.aesh.command.option.OptionList;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.parser.SubcommandNotFoundException;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.shell.Shell;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.terminal.Connection;
import org.aesh.terminal.formatting.TerminalString;
import org.aesh.terminal.tty.TtyDetect;
import org.aesh.terminal.utils.Config;
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
    private boolean redirectStderr;
    private String completionProgramName;
    private boolean dynamicComplete;
    private CommandNotFoundHandler commandNotFoundHandler;
    private Shell shell;
    private boolean programStatusEnabled;
    private String programStatusAppName;
    private Connection programStatusConnection;
    private org.aesh.command.invocation.CommandInvocationProvider commandInvocationProvider;
    private org.aesh.command.invocation.CommandInvocationBuilder commandInvocationBuilder;
    private org.aesh.command.converter.ConverterInvocationProvider converterInvocationProvider;
    private org.aesh.command.validator.ValidatorInvocationProvider validatorInvocationProvider;
    private org.aesh.console.AeshContext aeshContext;
    private Consumer<String> commandOutputHandler;
    private boolean lazyStartup = true;
    private boolean rootRegistered;
    private CommandContainerBuilder<?> customContainerBuilder;
    private DefaultValueProvider pendingDefaultValueProvider;
    private Class<? extends Command> pendingCommandClass;
    private Command pendingCommandInstance;
    private AnsiMode ansiMode = AnsiMode.AUTO;
    private UsageErrorMode usageErrorMode = UsageErrorMode.FULL_HELP;
    private EnumSet<HelpFeature> helpDefaults = EnumSet.noneOf(HelpFeature.class);
    private ArgumentsSection argumentsSectionDefault = ArgumentsSection.OFF;

    private AeshRuntimeRunner() {
    }

    public static AeshRuntimeRunner builder() {
        return new AeshRuntimeRunner();
    }

    /**
     * How ANSI colors are decided for help and other output.
     */
    public enum AnsiMode {
        /**
         * Decide from the environment: no colors when {@code NO_COLOR} is set
         * or stdout is not a terminal, colors when {@code FORCE_COLOR} (or
         * {@code CLICOLOR_FORCE}) is set or stdout is a terminal.
         */
        AUTO,
        /** Always use ANSI colors, unless {@code NO_COLOR} is set. */
        ON,
        /** Never use ANSI colors. */
        OFF;

        /**
         * Resolve this mode against the real environment (stdout terminal
         * probe plus {@code System.getenv}). Direct
         * {@code CommandRuntime} users can pass the result to
         * {@code AeshCommandRuntimeBuilder.ansiColors(...)} so
         * {@code mycli --help > help.txt} stays plain text without the
         * runner.
         *
         * @return true when help and other output should use ANSI colors
         */
        public boolean resolve() {
            return resolveAnsiColors(this, TtyDetect::isStdoutTty, System::getenv);
        }

        /**
         * Resolve this mode with injectable terminal and environment probes,
         * so tests can pass a fixed map instead of touching the environment.
         *
         * @param stdoutTty whether stdout is a terminal, consulted lazily
         * @param getenv environment lookup
         * @return true when help and other output should use ANSI colors
         */
        public boolean resolve(Supplier<Boolean> stdoutTty, Function<String, String> getenv) {
            return resolveAnsiColors(this, stdoutTty, getenv);
        }

        /**
         * Whether ANSI colors survive an output redirect, against the real
         * environment. Only explicit {@code ON} or
         * {@code FORCE_COLOR}/{@code CLICOLOR_FORCE} force them;
         * {@code NO_COLOR} always wins.
         *
         * @return true when redirected output should keep ANSI colors
         */
        public boolean resolveWhenRedirected() {
            return resolveForceWhenRedirected(this, System::getenv);
        }

        /**
         * Whether ANSI colors survive an output redirect, with an injectable
         * environment lookup for tests.
         *
         * @param getenv environment lookup
         * @return true when redirected output should keep ANSI colors
         */
        public boolean resolveWhenRedirected(Function<String, String> getenv) {
            return resolveForceWhenRedirected(this, getenv);
        }
    }

    /**
     * Control ANSI colors. The default {@link AnsiMode#AUTO} keeps colors in
     * a terminal and drops them when stdout is redirected (piped to a file,
     * CI logs), so {@code mycli --help > help.txt} stays plain text.
     *
     * @param ansiMode the ANSI mode, or null for {@link AnsiMode#AUTO}
     */
    public AeshRuntimeRunner ansiMode(AnsiMode ansiMode) {
        this.ansiMode = ansiMode != null ? ansiMode : AnsiMode.AUTO;
        return this;
    }

    /**
     * What follows a usage error (or a command failure that prints help).
     */
    public enum UsageErrorMode {
        /** Today's behavior: the error plus the full help of the command. */
        FULL_HELP,
        /** The error, the {@code Usage:} block, and a {@code --help} hint. */
        USAGE,
        /** Like {@code USAGE} but collapsed to {@code Usage: cmd [OPTIONS]}. */
        COLLAPSED_USAGE,
        /** Only the error and the {@code --help} hint, no usage. */
        HINT_ONLY
    }

    /**
     * Control what the runner prints after a usage error (unknown option,
     * missing value) or a command failure that prints help. The default
     * {@link UsageErrorMode#FULL_HELP} prints the error plus the full help,
     * exactly as before. The other modes print a {@code Try '<command>
     * --help' for more information.} hint instead of (or after) the
     * {@code Usage:} block alone, so a typo on a 70-option command no longer
     * scrolls the error away. {@code --help} itself always keeps the full
     * list.
     *
     * @param usageErrorMode the error-output mode, or null for
     *        {@link UsageErrorMode#FULL_HELP}
     */
    public AeshRuntimeRunner usageErrorMode(UsageErrorMode usageErrorMode) {
        this.usageErrorMode = usageErrorMode != null ? usageErrorMode : UsageErrorMode.FULL_HELP;
        return this;
    }

    /**
     * Fleet-wide help toggles applied when a command leaves them
     * {@code INHERIT} (see {@link Toggle} and {@link HelpFeature}).
     * Useful for large CLIs instead of repeating the same attributes on
     * every command; per-command {@code ON}/{@code OFF} always wins.
     */
    public AeshRuntimeRunner helpDefaults(EnumSet<HelpFeature> helpDefaults) {
        this.helpDefaults = helpDefaults != null ? helpDefaults : EnumSet.noneOf(HelpFeature.class);
        this.registryBuilder.helpDefaults(helpDefaults);
        return this;
    }

    /**
     * Fleet-wide arguments-section placement applied when a command leaves it
     * {@code INHERIT} (see {@link ArgumentsSection}). Useful for large CLIs
     * instead of repeating the attribute on every command; an explicit value
     * always wins.
     */
    public AeshRuntimeRunner argumentsSectionDefault(ArgumentsSection argumentsSectionDefault) {
        this.argumentsSectionDefault = argumentsSectionDefault != null ? argumentsSectionDefault
                : ArgumentsSection.OFF;
        this.registryBuilder.argumentsSectionDefault(argumentsSectionDefault);
        return this;
    }

    /**
     * Set a custom CommandContainerBuilder for command creation.
     * This allows frameworks (e.g., CDI, Spring) to inject dependencies
     * into subcommands and option service providers.
     */
    public AeshRuntimeRunner containerBuilder(CommandContainerBuilder<?> containerBuilder) {
        if (lazyStartup && !(containerBuilder instanceof AeshCommandContainerBuilder))
            throw new IllegalStateException(
                    "Only AeshCommandContainerBuilder subclasses are supported with lazy startup");
        customContainerBuilder = containerBuilder;
        this.registryBuilder.containerBuilder(containerBuilder);
        return this;
    }

    public AeshRuntimeRunner lazyStartup(boolean lazyStartup) {
        if (lazyStartup && rootRegistered)
            throw new IllegalStateException("lazyStartup must be configured before registering a command");
        if (lazyStartup && customContainerBuilder != null
                && !(customContainerBuilder instanceof AeshCommandContainerBuilder))
            throw new IllegalStateException(
                    "Only AeshCommandContainerBuilder subclasses are supported with lazy startup");
        this.lazyStartup = lazyStartup;
        if (!lazyStartup) {
            materializePendingRoot();
            try {
                registryBuilder.materializeAll();
            } catch (CommandRegistryException e) {
                throw new RuntimeException("Exception while building command: " + e.getMessage());
            }
        }
        return this;
    }

    public AeshRuntimeRunner command(Class<? extends Command> command) {
        if (rootRegistered)
            throw new IllegalStateException("AeshRuntimeRunner supports a single root command");
        if (lazyStartup) {
            pendingCommandClass = command;
            rootRegistered = true;
            return this;
        }
        try {
            registryBuilder.command(command);
        } catch (CommandRegistryException e) {
            throw new RuntimeException("Exception while building command: " + e.getMessage());
        }
        rootRegistered = true;
        return this;
    }

    @SuppressWarnings("unchecked")
    public AeshRuntimeRunner command(Command commandInstance) {
        if (rootRegistered)
            throw new IllegalStateException("AeshRuntimeRunner supports a single root command");
        if (lazyStartup) {
            pendingCommandInstance = commandInstance;
            pendingCommandClass = commandInstance.getClass();
            rootRegistered = true;
            return this;
        }
        try {
            registryBuilder.command(commandInstance);
        } catch (CommandRegistryException e) {
            throw new RuntimeException("Exception while building command: " + e.getMessage());
        }
        rootRegistered = true;
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

    /**
     * Silence the dynamic-completion callback's stderr in the generated
     * script, so JVM startup noise (e.g. incubator module warnings, which no
     * JVM flag suppresses) does not paint over the prompt during completion.
     * Applies to bash/zsh/fish; PowerShell scripts already redirect stderr.
     *
     * @param redirectStderr whether to append the shell-appropriate stderr
     *        redirect to the callback invocation
     * @return this builder
     */
    public AeshRuntimeRunner redirectStderr(boolean redirectStderr) {
        this.redirectStderr = redirectStderr;
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
        pendingDefaultValueProvider = provider;
        return this;
    }

    /**
     * Set a handler for command-not-found errors.
     * The handler receives the command line and a {@code Consumer<String>}
     * for writing output (backed by {@code System.err} in this runner).
     */
    public AeshRuntimeRunner commandNotFoundHandler(CommandNotFoundHandler handler) {
        this.commandNotFoundHandler = handler;
        return this;
    }

    /**
     * Set a custom Shell for command execution.
     * When not set, a lazy Shell is created that initializes a
     * TerminalConnection on first access to size() or connection().
     */
    public AeshRuntimeRunner shell(Shell shell) {
        this.shell = shell;
        return this;
    }

    /**
     * Opt-in OSC 7501 program-status reporting for explicit invocation
     * hooks. Disabled by default; see
     * {@code AeshCommandRuntimeBuilder#enableProgramStatus}.
     *
     * @param enable true to enable explicit reporting
     * @return this runner
     * @since 3.18.3
     */
    public AeshRuntimeRunner enableProgramStatus(boolean enable) {
        this.programStatusEnabled = enable;
        return this;
    }

    /**
     * Stable application name reported with program-status records.
     *
     * @param appName the application name, or null for none
     * @return this runner
     * @since 3.18.3
     */
    public AeshRuntimeRunner programStatusAppName(String appName) {
        this.programStatusAppName = appName;
        return this;
    }

    /**
     * Terminal connection for program-status reports. Must be supplied
     * explicitly — it is never discovered from shells, so no terminal is
     * ever initialized for reporting.
     *
     * @param connection the terminal connection, or null
     * @return this runner
     * @since 3.18.3
     */
    public AeshRuntimeRunner programStatusConnection(Connection connection) {
        this.programStatusConnection = connection;
        return this;
    }

    /**
     * Set a custom CommandInvocationProvider that wraps/enhances the default
     * CommandInvocation with a framework-specific subtype (e.g., for CDI injection).
     */
    public AeshRuntimeRunner commandInvocationProvider(org.aesh.command.invocation.CommandInvocationProvider<?> provider) {
        this.commandInvocationProvider = provider;
        return this;
    }

    /**
     * Set a custom CommandInvocationBuilder that creates CommandInvocation instances.
     * This is a lower-level hook than {@link #commandInvocationProvider} -- use it
     * when you need to replace the CommandInvocation factory entirely rather than
     * just wrapping the result.
     */
    public AeshRuntimeRunner commandInvocationBuilder(org.aesh.command.invocation.CommandInvocationBuilder<?> builder) {
        this.commandInvocationBuilder = builder;
        return this;
    }

    /**
     * Set a custom ConverterInvocationProvider for enriching the context
     * available to option value converters during parsing.
     */
    public AeshRuntimeRunner converterInvocationProvider(org.aesh.command.converter.ConverterInvocationProvider provider) {
        this.converterInvocationProvider = provider;
        return this;
    }

    /**
     * Set a custom ValidatorInvocationProvider for enriching the context
     * available to option value validators during parsing.
     */
    public AeshRuntimeRunner validatorInvocationProvider(org.aesh.command.validator.ValidatorInvocationProvider provider) {
        this.validatorInvocationProvider = provider;
        return this;
    }

    /**
     * Set a custom AeshContext for the command execution environment.
     * When not set, a default context is created with the user's current
     * working directory.
     */
    public AeshRuntimeRunner aeshContext(org.aesh.console.AeshContext context) {
        this.aeshContext = context;
        return this;
    }

    /**
     * Capture what aesh itself writes through the invocation shell (command
     * output and the generated {@code --help}) in addition to the shell
     * receiving it. Useful for applications with their own output: printers
     * tests capture, embedded shells, or tools returning command output to an
     * AI agent — without implementing {@code Shell} for the four write
     * methods.
     *
     * @param commandOutputHandler receives every written string, or null to disable
     * @return this runner
     */
    public AeshRuntimeRunner commandOutputHandler(Consumer<String> commandOutputHandler) {
        this.commandOutputHandler = commandOutputHandler;
        return this;
    }

    private void materializePendingRoot() {
        if (pendingCommandClass == null && pendingCommandInstance == null)
            return;
        try {
            if (lazyStartup) {
                AeshCommandContainerBuilder<?> builder;
                if (customContainerBuilder instanceof AeshCommandContainerBuilder) {
                    builder = (AeshCommandContainerBuilder<?>) customContainerBuilder;
                } else {
                    builder = new AeshCommandContainerBuilder<>();
                    registryBuilder.containerBuilder(builder);
                }
                builder.setLazyChildResolution(true);
                if (pendingDefaultValueProvider != null)
                    builder.setDefaultValueProvider(pendingDefaultValueProvider);
            }
            if (pendingCommandInstance != null)
                registryBuilder.command(pendingCommandInstance);
            else
                registryBuilder.command(pendingCommandClass);
        } catch (CommandRegistryException e) {
            throw new RuntimeException("Exception while building command: " + e.getMessage());
        }
        pendingCommandClass = null;
        pendingCommandInstance = null;
    }

    private CommandResult failFastOnUnknownSubcommand(boolean colors) {
        if (!lazyStartup || completionShellType != null || dynamicCompletionShellType != null || dynamicComplete)
            return null;
        if (pendingCommandClass == null)
            return null;
        try {
            LazyRouteResolver.validate(pendingCommandClass, args);
            return null;
        } catch (SubcommandNotFoundException e) {
            if (commandNotFoundHandler != null) {
                String fullLine = LazyRouteResolver.commandName(pendingCommandClass)
                        + (args != null && args.length > 0
                                ? " " + String.join(" ", args)
                                : "");
                commandNotFoundHandler.handleCommandNotFound(fullLine, System.err::println,
                        e.getUnknownSubcommand(), e.getAvailableSubcommands());
            }
            System.err.println(e.getMessage());
            String help = LazyHelp.renderUnknownSubcommandHelp(pendingCommandClass, args, colors, helpDefaults,
                    argumentsSectionDefault);
            if (help != null)
                System.err.println(help);
            return CommandResult.USAGE_ERROR;
        }
    }

    /**
     * Decide whether help and other output uses ANSI colors.
     * {@code NO_COLOR} always wins (colors off). An explicit mode wins over
     * the environment otherwise. In {@link AnsiMode#AUTO},
     * {@code FORCE_COLOR} (or {@code CLICOLOR_FORCE}) forces colors on;
     * otherwise colors follow stdout: on for a terminal, off when redirected.
     * Public so applications that run commands through
     * {@code CommandRuntime} directly can apply the same rule without the
     * runner.
     *
     * @param mode the configured mode
     * @param stdoutTty whether stdout is a terminal, consulted lazily
     * @param getenv environment lookup, so tests can pass a fixed map
     */
    public static boolean resolveAnsiColors(AnsiMode mode,
            Supplier<Boolean> stdoutTty,
            Function<String, String> getenv) {
        if (getenv.apply("NO_COLOR") != null)
            return false;
        if (mode == AnsiMode.OFF)
            return false;
        if (mode == AnsiMode.ON)
            return true;
        String force = getenv.apply("FORCE_COLOR");
        if (force == null)
            force = getenv.apply("CLICOLOR_FORCE");
        if (force != null && !force.isEmpty() && !"0".equals(force))
            return true;
        return stdoutTty.get();
    }

    /**
     * Whether ANSI colors survive an output redirect to a file or pipe.
     * Only explicit {@code ON} or {@code FORCE_COLOR}/{@code CLICOLOR_FORCE}
     * force them; {@code NO_COLOR} always wins. Public alongside
     * {@link #resolveAnsiColors} for direct {@code CommandRuntime} users.
     */
    public static boolean resolveForceWhenRedirected(AnsiMode mode,
            Function<String, String> getenv) {
        if (getenv.apply("NO_COLOR") != null)
            return false;
        if (mode == AnsiMode.OFF)
            return false;
        if (mode == AnsiMode.ON)
            return true;
        String force = getenv.apply("FORCE_COLOR");
        if (force == null)
            force = getenv.apply("CLICOLOR_FORCE");
        return force != null && !force.isEmpty() && !"0".equals(force);
    }

    @SuppressWarnings("unchecked")
    public CommandResult execute() {
        boolean colors = resolveAnsiColors(ansiMode, TtyDetect::isStdoutTty, System::getenv);
        // Errors and usage go to stderr, which may be redirected on its own
        boolean stderrColors = resolveAnsiColors(ansiMode, TtyDetect::isStderrTty, System::getenv);
        CommandResult fastFailure = failFastOnUnknownSubcommand(stderrColors);
        if (fastFailure != null)
            return fastFailure;
        if (lazyStartup && pendingCommandClass != null
                && completionShellType == null && dynamicCompletionShellType == null && !dynamicComplete) {
            String help = LazyHelp.render(pendingCommandClass, args, colors, helpDefaults, argumentsSectionDefault);
            if (help != null) {
                if (commandOutputHandler != null)
                    commandOutputHandler.accept(help + Config.getLineSeparator());
                System.out.println(help);
                return CommandResult.SUCCESS;
            }
        }
        materializePendingRoot();
        if (!lazyStartup && pendingDefaultValueProvider != null)
            registryBuilder.defaultValueProvider(pendingDefaultValueProvider);
        // Render help without ANSI when stdout is redirected: parsers built
        // from now on get the mode at build time, already-built ones in
        // create(), so lazy startup never pays for an early materialization.
        registryBuilder.ansiColors(colors);
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

        // Create a lazy Shell if none was provided — only initializes
        // TerminalConnection when size() or connection() is actually called.
        Shell effectiveShell = this.shell != null
                ? this.shell
                : new LazyTerminalShell();

        AeshCommandRuntimeBuilder runtimeBuilder = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(commandRegistry)
                .shell(effectiveShell)
                .ansiWhenRedirected(resolveForceWhenRedirected(ansiMode, System::getenv))
                .enableProgramStatus(programStatusEnabled)
                .programStatusAppName(programStatusAppName)
                .programStatusConnection(programStatusConnection);
        if (commandNotFoundHandler != null)
            runtimeBuilder.commandNotFoundHandler(commandNotFoundHandler);
        if (commandInvocationProvider != null)
            runtimeBuilder.commandInvocationProvider(commandInvocationProvider);
        if (commandInvocationBuilder != null)
            runtimeBuilder.commandInvocationBuilder(commandInvocationBuilder);
        if (converterInvocationProvider != null)
            runtimeBuilder.converterInvocationProvider(converterInvocationProvider);
        if (validatorInvocationProvider != null)
            runtimeBuilder.validatorInvocationProvider(validatorInvocationProvider);
        if (aeshContext != null)
            runtimeBuilder.aeshContext(aeshContext);
        if (commandOutputHandler != null)
            runtimeBuilder.commandOutputHandler(commandOutputHandler);
        CommandRuntime runtime = runtimeBuilder.build();

        String commandName = (String) commandRegistry.getAllCommandNames().iterator().next();

        try {
            return runtime.executeCommand(commandName, args);
        } catch (CommandDefinitionException e) {
            System.err.println(e.getMessage());
            return CommandResult.FAILURE;
        } catch (CommandNotFoundException e) {
            if (commandNotFoundHandler != null) {
                String fullLine = commandName + (args != null && args.length > 0
                        ? " " + String.join(" ", args)
                        : "");
                commandNotFoundHandler.handleCommandNotFound(fullLine, System.err::println,
                        e.getCommandName(), commandRegistry.getAllCommandNames());
            } else {
                System.err.println("Command not found: " + commandName);
            }
            return CommandResult.COMMAND_NOT_FOUND;
        } catch (CommandLineParserException | OptionValidatorException e) {
            // For subcommand-not-found, call the handler first for "did you mean?"
            if (e instanceof SubcommandNotFoundException
                    && commandNotFoundHandler != null) {
                SubcommandNotFoundException snfe = (SubcommandNotFoundException) e;
                String fullLine = commandName + (args != null && args.length > 0
                        ? " " + String.join(" ", args)
                        : "");
                commandNotFoundHandler.handleCommandNotFound(fullLine, System.err::println,
                        snfe.getUnknownSubcommand(), snfe.getAvailableSubcommands());
            }
            showHelp(runtime, commandName, args, e, stderrColors);
            return CommandResult.USAGE_ERROR;
        } catch (CommandException | CommandValidatorException e) {
            showHelp(runtime, commandName, args, e, stderrColors);
            return CommandResult.FAILURE;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CommandResult.INTERRUPTED;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return CommandResult.FAILURE;
        } finally {
            // Detach the per-call runtime from the registry listeners (#666)
            // and close the lazy shell to restore terminal attributes if a
            // TerminalConnection was initialized during command execution
            runtime.close();
            if (effectiveShell instanceof LazyTerminalShell) {
                ((LazyTerminalShell) effectiveShell).close();
            }
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
                    .generateDynamic(container.getParser(), programName, redirectStderr);
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
            // If completing a later segment of a separated option value, re-attach the
            // typed prefix: shells filter the whole current word and replace it wholesale,
            // while the completion engine only knows the last segment (#760).
            candidates = qualifySegmentCandidates(commandRegistry, commandName, candidates);

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
                CompletionFallback fallback = resolveOptionValueFallback(commandRegistry, commandName);
                if (fallback == null)
                    fallback = resolvePositionalFallback(commandRegistry, commandName);
                if (fallback == CompletionFallback.FILES) {
                    System.out.println("__aesh_file__");
                } else if (fallback == CompletionFallback.DIRECTORIES) {
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
     * If the cursor is on an option value (empty or bare word following a bare
     * value-taking option token), return that option. Otherwise return null.
     */
    @SuppressWarnings("unchecked")
    private ProcessedOption findOptionValueTarget(CommandRegistry<CommandInvocation> commandRegistry,
            String commandName) {
        if (args == null || args.length < 2)
            return null;
        String current = args[args.length - 1];
        if (!current.isEmpty() && current.charAt(0) == '-')
            return null;
        String previous = args[args.length - 2];
        if (previous.length() < 2 || previous.charAt(0) != '-' || previous.indexOf('=') >= 0)
            return null;
        boolean longOption = previous.charAt(1) == '-';
        String optToken = previous.substring(longOption ? 2 : 1);
        if (optToken.isEmpty() || (!longOption && optToken.length() != 1))
            return null;
        try {
            CommandContainer<CommandInvocation> container = (CommandContainer<CommandInvocation>) commandRegistry
                    .getCommand(commandName, "");
            CommandLineParser<CommandInvocation> scopedParser = findScopedParser(container.getParser(), args);
            ProcessedCommand<?, ?> command = scopedParser.getProcessedCommand();
            if (longOption)
                return command.findLongOptionNoActivatorCheck(optToken);
            return command.findOptionNoActivatorCheck(optToken);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * If the cursor is on an option value, return the fallback that option drives. An
     * option with a user-supplied completer owns its value space, so file
     * completion is never forced for it; otherwise the option's own fallback
     * applies. Return null when the cursor is not on an option value, so the
     * positional fallback applies instead (#759).
     */
    private CompletionFallback resolveOptionValueFallback(CommandRegistry<CommandInvocation> commandRegistry,
            String commandName) {
        ProcessedOption option = findOptionValueTarget(commandRegistry, commandName);
        if (option == null || !option.hasValue())
            return null;
        if (hasCustomCompleter(option))
            return CompletionFallback.NONE;
        return option.getCompleteFallback();
    }

    /**
     * Whether the option has a user-supplied completer. Framework-attached
     * completers (file completion for File/Resource/Path types, allowed-values
     * completion) do not count: file forcing stays driven by the fallback.
     */
    private static boolean hasCustomCompleter(ProcessedOption option) {
        OptionCompleter<?> completer = option.completer();
        return completer != null
                && !(completer instanceof FileOptionCompleter)
                && !(completer instanceof DefaultValueOptionCompleter);
    }

    /**
     * If the cursor is on a later segment of a separated option value (e.g. the
     * {@code ja} in {@code --opt jbang,ja}), re-attach the typed prefix to every
     * candidate ({@code jbang,javahome}). Shells filter the whole current word
     * and replace it wholesale, while the completion engine only knows the last
     * segment, so bare segment values would be discarded (#760). The first
     * segment, space-separated values and {@code NO_SEPARATOR} options are
     * returned unchanged.
     */
    private List<TerminalString> qualifySegmentCandidates(CommandRegistry<CommandInvocation> commandRegistry,
            String commandName, List<TerminalString> candidates) {
        if (candidates.isEmpty())
            return candidates;
        String current = args != null && args.length > 0 ? args[args.length - 1] : "";
        if (current.isEmpty())
            return candidates;
        ProcessedOption option = findOptionValueTarget(commandRegistry, commandName);
        if (option == null || !option.hasValue())
            return candidates;
        char separator = option.getValueSeparator();
        if (separator == ' ' || separator == OptionList.NO_SEPARATOR)
            return candidates;
        int split = current.lastIndexOf(separator);
        if (split < 0)
            return candidates;
        String prefix = current.substring(0, split + 1);
        List<TerminalString> qualified = new ArrayList<>(candidates.size());
        for (TerminalString candidate : candidates)
            qualified.add(candidate.cloneRenderingAttributes(prefix + candidate.getCharacters()));
        return qualified;
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
     * Handles --aesh-doc [asciidoc|markdown|skill] [--output-dir dir] [-- path...]:
     * generates documentation to stdout or one file per command.
     * <p>
     * Tokens after a {@code --} separator select a subcommand subtree,
     * mirroring {@code --aesh-complete -- <words>}.
     * <p>
     * Examples:
     * <ul>
     * <li>{@code --aesh-doc} — AsciiDoc to stdout (default)</li>
     * <li>{@code --aesh-doc asciidoc} — AsciiDoc to stdout</li>
     * <li>{@code --aesh-doc markdown} — Markdown to stdout</li>
     * <li>{@code --aesh-doc asciidoc --output-dir docs} — one file per
     * command under {@code docs} (must already exist)</li>
     * <li>{@code --aesh-doc skill -- run} — SKILL doc for the 'run' subcommand only</li>
     * <li>{@code --aesh-doc markdown -- jdk install} — Markdown doc for a nested subcommand</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private CommandResult handleBuiltinDoc(CommandRegistry commandRegistry) {
        org.aesh.command.DocFormat format = org.aesh.command.DocFormat.ASCIIDOC;
        File outputDir = null;

        int separatorIndex = -1;
        for (int i = 1; i < args.length; i++) {
            if ("--".equals(args[i])) {
                separatorIndex = i;
                break;
            }
        }
        int docEnd = separatorIndex >= 0 ? separatorIndex : args.length;

        for (int i = 1; i < docEnd; i++) {
            if ("--output-dir".equals(args[i])) {
                if (i + 1 >= docEnd) {
                    System.err.println("Missing directory after --output-dir");
                    return CommandResult.FAILURE;
                }
                outputDir = new File(args[++i]);
                continue;
            }
            try {
                format = org.aesh.command.DocFormat.valueOf(args[i].toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Unknown doc format: " + args[i]
                        + ". Supported: asciidoc, markdown, skill");
                return CommandResult.FAILURE;
            }
        }

        if (outputDir != null && !outputDir.isDirectory()) {
            System.err.println("Output directory does not exist: " + outputDir);
            return CommandResult.FAILURE;
        }

        try {
            String commandName = (String) commandRegistry.getAllCommandNames().iterator().next();
            CommandContainer<CommandInvocation> container = (CommandContainer<CommandInvocation>) commandRegistry
                    .getCommand(commandName, "");

            String programName = completionProgramName != null ? completionProgramName : commandName;
            CommandLineParser<?> parser = container.getParser();
            if (separatorIndex >= 0) {
                for (int i = separatorIndex + 1; i < args.length; i++) {
                    CommandLineParser<?> child = parser.getChildParser(args[i]);
                    if (child == null) {
                        System.err.println("Unknown command: " + args[i]);
                        return CommandResult.FAILURE;
                    }
                    parser = child;
                    programName += " " + child.getProcessedCommand().name();
                }
            }

            org.aesh.util.doc.DocumentationGenerator.Builder builder = org.aesh.util.doc.DocumentationGenerator
                    .builder()
                    .parser(parser)
                    .programName(programName)
                    .format(format);
            if (outputDir != null) {
                builder.outputDir(outputDir).generate();
                return CommandResult.SUCCESS;
            }
            String doc = builder.generateSingle();
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
        if (Config.isWindows())
            return new java.io.BufferedReader(new java.io.InputStreamReader(System.in)).readLine();
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

    @SuppressWarnings("unchecked")
    private void showHelp(CommandRuntime runtime, String commandName, String[] args, Exception e,
            boolean colors) {
        System.err.println(e.getMessage());
        String helpLine = resolveHelpCommand(runtime, commandName, args);
        if (usageErrorMode == UsageErrorMode.FULL_HELP) {
            if (!colors) {
                // Usage errors go to stderr, which may be redirected on its own.
                // Already resolved above, so this never builds.
                try {
                    CommandContainer<CommandInvocation> container = (CommandContainer<CommandInvocation>) runtime
                            .getCommandRegistry().getCommand(commandName, "");
                    container.getParser().updateAnsiMode(false);
                } catch (CommandNotFoundException ignored) {
                }
            }
            System.err.println(runtime.commandInfo(helpLine));
            return;
        }
        try {
            CommandContainer<CommandInvocation> container = (CommandContainer<CommandInvocation>) runtime
                    .getCommandRegistry().getCommand(commandName, "");
            if (!colors) {
                container.getParser().updateAnsiMode(false);
            }
            if (usageErrorMode != UsageErrorMode.HINT_ONLY) {
                System.err.print(container.printSynopsis(helpLine,
                        usageErrorMode == UsageErrorMode.COLLAPSED_USAGE));
            }
        } catch (CommandNotFoundException ignored) {
        }
        System.err.println("Try '" + helpLine + " --help' for more information.");
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
