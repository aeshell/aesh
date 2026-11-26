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

package org.aesh.command;

import java.util.EnumSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.aesh.command.activator.CommandActivatorProvider;
import org.aesh.command.activator.OptionActivatorProvider;
import org.aesh.command.completer.CompleterInvocationProvider;
import org.aesh.command.converter.ConverterInvocationProvider;
import org.aesh.command.impl.AeshCommandRuntime;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.impl.invocation.DefaultCommandInvocationBuilder;
import org.aesh.command.impl.registry.MutableCommandRegistryImpl;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationBuilder;
import org.aesh.command.invocation.CommandInvocationProvider;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.operator.OperatorType;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.shell.Shell;
import org.aesh.command.validator.ValidatorInvocationProvider;
import org.aesh.console.AeshContext;
import org.aesh.console.DefaultAeshContext;
import org.aesh.terminal.Connection;

/**
 *
 * @author Aesh team
 */
public class AeshCommandRuntimeBuilder<CI extends CommandInvocation> {

    /**
     * The full operator set, computed on demand. Replaces the eager
     * {@code ALL_OPERATORS} constant, whose class initializer ran
     * {@code EnumSet.allOf} on every start — dragging in ~190 JDK
     * invoke/classfile/constant classes through {@code MethodHandleAccessorFactory}
     * even for CLIs that never use operators (#798).
     *
     * @return a fresh mutable set with every operator enabled
     */
    public static EnumSet<OperatorType> allOperators() {
        return EnumSet.allOf(OperatorType.class);
    }

    /**
     * Shared stateless default: {@code build()} used to allocate a fresh
     * anonymous provider per runtime even when nothing was customized (#796).
     */
    @SuppressWarnings("rawtypes")
    private static final CommandInvocationProvider DEFAULT_COMMAND_INVOCATION_PROVIDER = new CommandInvocationProvider() {
    };

    /**
     * Shared immutable default composite for builds that configure no custom
     * invocation providers — every slot is a stateless default (#796).
     */
    private static final InvocationProviders DEFAULT_INVOCATION_PROVIDERS = new AeshInvocationProviders();

    private CommandRegistry<CI> registry;
    private CommandInvocationProvider<CI> commandInvocationProvider;
    private CommandNotFoundHandler commandNotFoundHandler;
    private CompleterInvocationProvider completerInvocationProvider;
    private ConverterInvocationProvider converterInvocationProvider;
    private ValidatorInvocationProvider validatorInvocationProvider;
    private OptionActivatorProvider optionActivatorProvider;
    private CommandActivatorProvider commandActivatorProvider;
    private InvocationProviders invocationProviders;
    private AeshContext ctx;
    private CommandInvocationBuilder<CI> commandInvocationBuilder;
    private Shell shell;

    private DefaultValueProvider defaultValueProvider;
    private boolean parseBrackets;
    private EnumSet<OperatorType> operators;
    private PipelineConfig pipelineConfig;
    private Consumer<String> commandOutputHandler;
    private boolean programStatusEnabled;
    private String programStatusAppName;
    private Connection programStatusConnection;
    /**
     * Keep ANSI colors when a command redirects its output to a file or pipe.
     * Off by default: redirected output is plain unless explicitly forced.
     */
    private boolean ansiWhenRedirected;
    /**
     * Whether parsers built by this runtime's registry render help with ANSI
     * colors. Null (default) leaves the registry alone; set false to drop
     * colors (for example when stdout is redirected). Applied to
     * already-built containers and to later builds.
     */
    private Boolean ansiColors;
    private HelpColorScheme helpColorScheme;
    /**
     * Lazily resolved stdout colors for {@code --help} rendering, see
     * {@link #stdoutColors(Supplier)}. Null (default) keeps the previous
     * behavior.
     */
    private Supplier<Boolean> stdoutColors;
    /**
     * Whether {@code @file} arguments are replaced by the arguments listed
     * in that file. Off by default.
     */
    private boolean argumentFiles;

    private AeshCommandRuntimeBuilder() {
    }

    public static <T extends CommandInvocation> AeshCommandRuntimeBuilder<T> builder() {
        return new AeshCommandRuntimeBuilder<>();
    }

    public AeshCommandRuntimeBuilder<CI> parseBrackets(boolean parseBrackets) {
        this.parseBrackets = parseBrackets;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> operators(EnumSet<OperatorType> operators) {
        this.operators = operators;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> pipelineConfig(PipelineConfig pipelineConfig) {
        this.pipelineConfig = pipelineConfig;
        return this;
    }

    /**
     * Opt-in OSC 7501 program-status reporting for explicit invocation
     * hooks. Disabled by default. No automatic lifecycle reports are
     * published on this path; the scope only makes
     * {@code CommandInvocation} reporting available.
     *
     * @param enabled true to enable explicit reporting
     * @return this builder
     * @since 3.18.3
     */
    public AeshCommandRuntimeBuilder<CI> enableProgramStatus(boolean enabled) {
        this.programStatusEnabled = enabled;
        return this;
    }

    /**
     * Stable application name reported with program-status records.
     *
     * @param appName the application name, or null for none
     * @return this builder
     * @since 3.18.3
     */
    public AeshCommandRuntimeBuilder<CI> programStatusAppName(String appName) {
        this.programStatusAppName = appName;
        return this;
    }

    /**
     * Terminal connection for program-status reports. Must be supplied
     * explicitly — it is never discovered from shells, so no terminal is
     * ever initialized for reporting.
     *
     * @param connection the terminal connection, or null
     * @return this builder
     * @since 3.18.3
     */
    public AeshCommandRuntimeBuilder<CI> programStatusConnection(Connection connection) {
        this.programStatusConnection = connection;
        return this;
    }

    /**
     * Keep ANSI colors when a command redirects its output. File redirects
     * always strip ANSI codes when writing; pipes preserve them, so this
     * decides what a downstream command receives. Off by default; forced on
     * by {@code AeshRuntimeRunner} with explicit {@code ON} or
     * {@code FORCE_COLOR}/{@code CLICOLOR_FORCE}.
     */
    public AeshCommandRuntimeBuilder<CI> ansiWhenRedirected(boolean ansiWhenRedirected) {
        this.ansiWhenRedirected = ansiWhenRedirected;
        return this;
    }

    /**
     * Set whether parsers built by this runtime's registry render help with
     * ANSI colors. Null (default) leaves the registry alone; set false to drop
     * colors (for example when stdout is redirected). Applied to
     * already-built containers in {@link #build()}
     * and to later builds, without forcing an early materialization.
     * Direct {@code CommandRuntime} users can compute the value with
     * {@code AeshRuntimeRunner.AnsiMode.AUTO.resolve()} so
     * {@code mycli --help > help.txt} stays plain text without the runner.
     *
     * @param ansiColors false to drop ANSI colors, true to keep them
     * @return this builder
     */
    public AeshCommandRuntimeBuilder<CI> ansiColors(boolean ansiColors) {
        this.ansiColors = ansiColors;
        return this;
    }

    /**
     * Set the help color scheme applied to parsers built by the registry
     * (and to already-built containers in {@link #build()}). Null (default)
     * leaves the registry's scheme or process default untouched.
     *
     * @param helpColorScheme the help color scheme, or null for the default
     * @return this builder
     */
    public AeshCommandRuntimeBuilder<CI> helpColorScheme(HelpColorScheme helpColorScheme) {
        this.helpColorScheme = helpColorScheme;
        return this;
    }

    /**
     * Lazily decide whether {@code --help} output uses ANSI colors when it is
     * rendered. Unlike {@link #ansiColors(boolean)}, the supplier is forced
     * only if help is actually printed without an output redirection, so
     * successful executions never pay for a terminal probe. Null (default)
     * keeps the previous behavior: the parser's built-time mode decides.
     * Used by {@code AeshRuntimeRunner} to defer its stdout probe (#783);
     * direct {@code CommandRuntime} users can pass
     * {@code AeshRuntimeRunner.AnsiMode.AUTO.resolve(...)} suppliers too.
     *
     * @param stdoutColors whether stdout help uses ANSI colors, consulted lazily
     * @return this builder
     */
    public AeshCommandRuntimeBuilder<CI> stdoutColors(Supplier<Boolean> stdoutColors) {
        this.stdoutColors = stdoutColors;
        return this;
    }

    /**
     * Replace {@code @file} arguments with the arguments listed in that file
     * (one per line or whitespace separated, quotes group words, {@code #}
     * starts a comment line, {@code @@} escapes a literal {@code @}). Off by
     * default; only whole pre-tokenized arguments are expanded.
     *
     * @param argumentFiles true to expand {@code @file} arguments
     * @return this builder
     */
    public AeshCommandRuntimeBuilder<CI> argumentFiles(boolean argumentFiles) {
        this.argumentFiles = argumentFiles;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> commandRegistry(CommandRegistry<CI> registry) {
        this.registry = registry;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> commandInvocationProvider(CommandInvocationProvider<CI> commandInvocationProvider) {
        this.commandInvocationProvider = commandInvocationProvider;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> commandNotFoundHandler(CommandNotFoundHandler commandNotFoundHandler) {
        this.commandNotFoundHandler = commandNotFoundHandler;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> completerInvocationProvider(
            CompleterInvocationProvider completerInvocationProvider) {
        this.completerInvocationProvider = completerInvocationProvider;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> converterInvocationProvider(
            ConverterInvocationProvider converterInvocationProvider) {
        this.converterInvocationProvider = converterInvocationProvider;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> validatorInvocationProvider(
            ValidatorInvocationProvider validatorInvocationProvider) {
        this.validatorInvocationProvider = validatorInvocationProvider;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> optionActivatorProvider(
            OptionActivatorProvider optionActivatorProvider) {
        this.optionActivatorProvider = optionActivatorProvider;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> commandActivatorProvider(
            CommandActivatorProvider commandActivatorProvider) {
        this.commandActivatorProvider = commandActivatorProvider;
        return this;
    }

    /**
     * Sets the full composite of invocation providers. Individual provider
     * setters called afterwards override just their slot; calling
     * {@link #settings(Settings)} afterwards replaces the whole composite.
     *
     * @param invocationProviders the composite providers, or null for defaults
     */
    public AeshCommandRuntimeBuilder<CI> invocationProviders(InvocationProviders invocationProviders) {
        this.invocationProviders = invocationProviders;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> shell(Shell shell) {
        this.shell = shell;
        return this;
    }

    @SuppressWarnings("unchecked")
    public AeshCommandRuntimeBuilder<CI> commandInvocationBuilder(CommandInvocationBuilder commandInvocationBuilder) {
        this.commandInvocationBuilder = commandInvocationBuilder;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> aeshContext(AeshContext ctx) {
        this.ctx = ctx;
        return this;
    }

    /**
     * Capture what aesh itself writes through the invocation shell (command
     * output and the generated {@code --help}) in addition to the shell
     * receiving it. Useful for applications with their own output: printers
     * tests capture, embedded shells, or tools returning command output to an
     * AI agent — without implementing {@code Shell} for the four write
     * methods. Without a shell on the runtime the output still goes to
     * {@code System.out} and is additionally passed to the handler.
     *
     * @param commandOutputHandler receives every written string, or null to disable
     * @return this builder
     */
    public AeshCommandRuntimeBuilder<CI> commandOutputHandler(Consumer<String> commandOutputHandler) {
        this.commandOutputHandler = commandOutputHandler;
        return this;
    }

    /**
     * Set a registry-level DefaultValueProvider that applies to all commands
     * that don't declare their own per-command provider via the annotation.
     */
    public AeshCommandRuntimeBuilder<CI> defaultValueProvider(DefaultValueProvider provider) {
        this.defaultValueProvider = provider;
        return this;
    }

    @SuppressWarnings("unchecked")
    public AeshCommandRuntimeBuilder<CI> settings(Settings<? extends CommandInvocation> settings) {
        this.commandInvocationProvider = (CommandInvocationProvider<CI>) settings.commandInvocationProvider();
        this.commandNotFoundHandler = settings.commandNotFoundHandler();
        this.completerInvocationProvider = settings.completerInvocationProvider();
        this.converterInvocationProvider = settings.converterInvocationProvider();
        this.validatorInvocationProvider = settings.validatorInvocationProvider();
        this.optionActivatorProvider = settings.optionActivatorProvider();
        this.commandActivatorProvider = settings.commandActivatorProvider();
        this.invocationProviders = settings.invocationProviders();
        this.registry = (CommandRegistry<CI>) settings.commandRegistry();
        this.ctx = settings.aeshContext();
        this.operators = settings.operatorParserEnabled() ? allOperators() : null;
        this.pipelineConfig = settings.pipelineConfig();
        this.commandOutputHandler = settings.commandOutputHandler();
        return this;
    }

    @SuppressWarnings("unchecked")
    public CommandRuntime<CI> build() {
        if (registry == null) {
            registry = new MutableCommandRegistryImpl<>();
        }

        if (defaultValueProvider != null && registry instanceof MutableCommandRegistryImpl) {
            MutableCommandRegistryImpl<CI> mutableRegistry = (MutableCommandRegistryImpl<CI>) registry;
            mutableRegistry.setDefaultValueProvider(defaultValueProvider);
            mutableRegistry.applyDefaultValueProvider();
        }

        if (ansiColors != null && registry instanceof MutableCommandRegistryImpl) {
            MutableCommandRegistryImpl<CI> mutableRegistry = (MutableCommandRegistryImpl<CI>) registry;
            mutableRegistry.setAnsiColors(ansiColors);
            mutableRegistry.applyAnsiColors();
        }

        if (helpColorScheme != null && registry instanceof MutableCommandRegistryImpl) {
            MutableCommandRegistryImpl<CI> mutableRegistry = (MutableCommandRegistryImpl<CI>) registry;
            mutableRegistry.setHelpColorScheme(helpColorScheme);
            mutableRegistry.applyHelpColorScheme();
        }

        if (commandInvocationProvider == null) {
            commandInvocationProvider = (CommandInvocationProvider<CI>) DEFAULT_COMMAND_INVOCATION_PROVIDER;
        }

        if (commandInvocationBuilder == null)
            commandInvocationBuilder = (CommandInvocationBuilder) new DefaultCommandInvocationBuilder(shell,
                    commandOutputHandler);

        if (ctx == null) {
            ctx = DefaultAeshContext.getDefault();
        }

        // A null operator set passes through: AeshCommandRuntime normalizes
        // it to an immutable empty set, so CLIs that never enable operators
        // skip the EnumSet machinery entirely (#798)

        InvocationProviders resolvedProviders = invocationProviders;
        if (resolvedProviders != null
                && (converterInvocationProvider != null || completerInvocationProvider != null
                        || validatorInvocationProvider != null || optionActivatorProvider != null
                        || commandActivatorProvider != null)) {
            // Individual setters called alongside a composite override just
            // their slot, falling back to the composite's providers.
            resolvedProviders = new AeshInvocationProviders(
                    converterInvocationProvider != null ? converterInvocationProvider
                            : resolvedProviders.getConverterProvider(),
                    completerInvocationProvider != null ? completerInvocationProvider
                            : resolvedProviders.getCompleterProvider(),
                    validatorInvocationProvider != null ? validatorInvocationProvider
                            : resolvedProviders.getValidatorProvider(),
                    optionActivatorProvider != null ? optionActivatorProvider
                            : resolvedProviders.getOptionActivatorProvider(),
                    commandActivatorProvider != null ? commandActivatorProvider
                            : resolvedProviders.getCommandActivatorProvider(),
                    resolvedProviders.fuzzyCompletion());
        } else if (resolvedProviders == null) {
            if (converterInvocationProvider == null && completerInvocationProvider == null
                    && validatorInvocationProvider == null && optionActivatorProvider == null
                    && commandActivatorProvider == null) {
                // No custom providers at all: share the immutable default
                // composite instead of allocating a fresh one per build (#796)
                resolvedProviders = DEFAULT_INVOCATION_PROVIDERS;
            } else {
                // Historic path: individuals only (nulls become defaults).
                resolvedProviders = new AeshInvocationProviders(converterInvocationProvider,
                        completerInvocationProvider, validatorInvocationProvider, optionActivatorProvider,
                        commandActivatorProvider);
            }
        }
        // Otherwise the composite is used as-is (identity preserved, no allocation).

        AeshCommandRuntime<CI> runtime = new AeshCommandRuntime<>(ctx, registry, commandInvocationProvider,
                commandNotFoundHandler, resolvedProviders,
                commandInvocationBuilder, parseBrackets, operators);
        if (pipelineConfig != null)
            runtime.setPipelineConfig(pipelineConfig);
        runtime.setProgramStatus(programStatusEnabled, programStatusAppName, programStatusConnection);
        runtime.setAnsiWhenRedirected(ansiWhenRedirected);
        runtime.setStdoutColors(stdoutColors);
        runtime.setArgumentFiles(argumentFiles);
        return runtime;
    }
}
