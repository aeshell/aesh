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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.aesh.command.activator.CommandActivator;
import org.aesh.command.impl.activator.NullCommandActivator;
import org.aesh.command.impl.provider.NullDefaultValueProvider;
import org.aesh.command.impl.provider.NullHelpSectionProvider;
import org.aesh.command.impl.result.NullResultHandler;
import org.aesh.command.impl.validator.NullCommandValidator;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.validator.CommandValidator;

/**
 * Defines a command class.
 *
 * <p>
 * The attributes that shape {@code --help} output are described together in the
 * <a href="/docs/aesh/customizing-help">Customizing help output</a> guide: layout
 * ({@link #collapseSynopsis()}, {@link #defaultOptionsFirst()},
 * {@link #argumentsSection()}, {@link #headerBeforeSynopsis()}, {@link #sortOptions()}), content
 * ({@link #header()}, {@link #footer()}, {@link #helpUrl()},
 * {@link #showDefaultValues()}, {@link #helpGroup()}) and dynamic sections
 * ({@link #helpSectionProvider()}).
 *
 * @author Aesh team
 */
@Inherited
@Retention(RUNTIME)
@Target(TYPE)
public @interface CommandDefinition {

    /**
     * Specifies the command name
     *
     * @return command name
     */
    String name();

    /**
     * Add any aliases connected to this command
     * Not required
     *
     * @return alias names
     */
    String[] aliases() default {};

    /**
     * Specify a description of the command
     *
     * @return command description
     */
    String description();

    /**
     * Automatically generate a help option for the command
     * Set to false by default
     *
     * @return generate help option
     */
    boolean generateHelp() default false;

    /**
     * Will prevent automatic parsing and validation of options and arguments for this command.
     * All values given after the command will be added to the field annotated with @Arguments.
     *
     * @return true if disable parsing is enabled
     */
    boolean disableParsing() default false;

    /**
     * If set will automatically generate a version option (--version, -v)
     * for the command with the given value
     *
     * @return version
     */
    String version() default "";

    /**
     * Specify a CommandValidator that will be called before the command is executed.
     *
     * @return CommandValidator
     */
    Class<? extends CommandValidator> validator() default NullCommandValidator.class;

    /**
     * Specify a ResultHandler that will be called after the command finished execution.
     *
     * @return ResultHandler
     */
    Class<? extends ResultHandler> resultHandler() default NullResultHandler.class;

    /**
     * Specify a CommandActivator which will verify if a command can be activated/accessed.
     *
     * @return CommandActivator
     */
    Class<? extends CommandActivator> activator() default NullCommandActivator.class;

    /**
     * Optional URL to the command's documentation.
     * When the terminal supports hyperlinks, this is shown as a clickable link in --help output.
     *
     * @return documentation URL
     *         <p>
     *         See the <a href="/docs/aesh/customizing-help">Customizing help output</a> guide.
     */
    String helpUrl() default "";

    /**
     * Specify a DefaultValueProvider that resolves option defaults at runtime.
     * Called during command population for options not explicitly set by the user.
     * If the provider returns non-null, it takes precedence over static defaultValue.
     *
     * @return DefaultValueProvider class
     */
    Class<? extends DefaultValueProvider> defaultValueProvider() default NullDefaultValueProvider.class;

    /**
     * Optional group heading under which this command appears in the parent's help output.
     * Commands with the same helpGroup value are displayed together under that heading.
     * Commands with no helpGroup appear under the default "Commands:" heading.
     *
     * @return help group heading
     *         <p>
     *         See the <a href="/docs/aesh/customizing-help">Customizing help output</a> guide.
     */
    String helpGroup() default "";

    /**
     * Specify a HelpSectionProvider that dynamically adds entries to help output.
     * Only invoked at help-render time — no startup cost.
     * When unset and the command class itself implements
     * {@code HelpSectionProvider}, the command instance is used.
     * An explicit value always wins over the command itself.
     *
     * @return HelpSectionProvider class
     *         <p>
     *         See the <a href="/docs/aesh/customizing-help">Customizing help output</a> guide.
     */
    Class<? extends HelpSectionProvider> helpSectionProvider() default NullHelpSectionProvider.class;

    /**
     * Text shown in the help after the usage line, one line per element, for example a summary of what the command
     * does. A {@link #helpSectionProvider()} that gives its own header is shown instead.
     *
     * @return the lines of the header
     *         <p>
     *         See the <a href="/docs/aesh/customizing-help">Customizing help output</a> guide.
     */
    String[] header() default {};

    /**
     * Text shown at the end of the help, one line per element, for example examples of the command. A
     * {@link #helpSectionProvider()} that gives its own footer is shown instead.
     *
     * @return the lines of the footer
     *         <p>
     *         See the <a href="/docs/aesh/customizing-help">Customizing help output</a> guide.
     */
    String[] footer() default {};

    /**
     * When set to true, option parsing stops after the first positional argument is consumed.
     * All remaining tokens are treated as positional arguments, even if they look like options.
     *
     * <p>
     * This is useful for commands that pass arguments through to another process.
     * For example: {@code run --verbose myscript.java -Dfoo=bar --help}
     * With stopAtFirstPositional=true, {@code -Dfoo=bar} and {@code --help} are treated as
     * positional arguments rather than being parsed as options of this command.
     *
     * @return true if option parsing should stop at the first positional argument
     */
    boolean stopAtFirstPositional() default false;

    /**
     * Controls option ordering in help output.
     * When true, options are sorted alphabetically by name.
     * When false (default), options keep declaration order, with optional
     * per-option {@code order} overrides.
     * <p>
     * See the <a href="/docs/aesh/customizing-help">Customizing help output</a> guide.
     */
    boolean sortOptions() default false;

    /**
     * Fail parsing when a single-value option is given more than once.
     * By default the last value wins, so {@code --level debug --level info}
     * runs with {@code info}. With this set to {@code ON}, repeating an option
     * is a usage error instead. Boolean flags, {@code @OptionList},
     * {@code @OptionGroup} and positional arguments are never affected.
     * {@code INHERIT} (the default) takes the value from the parent command,
     * else the fleet default (see {@code HelpFeature}).
     * <p>
     * See the <a href="/docs/aesh/customizing-help">Customizing help output</a> guide.
     */
    Toggle failOnDuplicateOptions() default Toggle.INHERIT;

    /**
     * Collapse the synopsis {@code Usage:} line to {@code [OPTIONS]} instead
     * of inlining every optional flag. Required options are still shown
     * explicitly; only optional flags collapse. The grouped Options table
     * below still lists every option in full.
     * <p>
     * Useful for option-heavy commands whose full synopsis would wrap
     * across many lines (e.g. {@code Usage: jbang [OPTIONS] [COMMAND]}).
     * {@code INHERIT} (the default) takes the value from the parent command,
     * else the fleet default (see {@code HelpFeature}).
     * <p>
     * See the <a href="/docs/aesh/customizing-help">Customizing help output</a> guide.
     */
    Toggle collapseSynopsis() default Toggle.INHERIT;

    /**
     * Show the default value of each option and positional argument in the help, on a line of its own under the
     * description: {@code Default: <value>}. Options without a default value get no line.
     * {@code INHERIT} (the default) takes the value from the parent command,
     * else the fleet default (see {@code HelpFeature}).
     * <p>
     * See the <a href="/docs/aesh/customizing-help">Customizing help output</a> guide.
     */
    Toggle showDefaultValues() default Toggle.INHERIT;

    /**
     * Print the default (ungrouped) {@code Options:} section before the
     * named {@code helpGroup} sections instead of after them, so the
     * command's own options come first.
     * {@code INHERIT} (the default) takes the value from the parent command,
     * else the fleet default (see {@code HelpFeature}).
     * <p>
     * See the <a href="/docs/aesh/customizing-help">Customizing help output</a> guide.
     */
    Toggle defaultOptionsFirst() default Toggle.INHERIT;

    /**
     * Where positional arguments render in help output: their own
     * {@code Arguments:} heading first (right after the usage line), last
     * (after all option groups), or not at all (trailing the last option
     * section as plain positionals).
     * {@code INHERIT} (the default) takes the value from the parent command,
     * else the fleet default.
     * <p>
     * See the <a href="/docs/aesh/customizing-help">Customizing help output</a> guide.
     */
    ArgumentsSection argumentsSection() default ArgumentsSection.INHERIT;

    /**
     * Print the help header (from a {@link HelpSectionProvider} or the
     * {@code header} attribute) before the usage line, right after the
     * one-line description, instead of between the usage line and the
     * options sections. Useful when the header is descriptive prose and
     * examples that read better ahead of the synopsis.
     * {@code INHERIT} (the default) takes the value from the parent command,
     * else the fleet default (see {@code HelpFeature}).
     * <p>
     * See the <a href="/docs/aesh/customizing-help">Customizing help output</a> guide.
     */
    Toggle headerBeforeSynopsis() default Toggle.INHERIT;

    /**
     * Default completion fallback for all options and arguments in this command.
     * Individual options can still override with their own {@code completeFallback}.
     * <p>
     * When set to a value other than {@code DEFAULT}, it replaces the type-based
     * heuristic (String/File/Path -> FILES) for options that don't declare their own.
     * <p>
     * Example: set to {@code NONE} to suppress file completion on all String options:
     *
     * <pre>
     * {@literal @}CommandDefinition(name = "myapp", completeFallback = CompletionFallback.NONE)
     * </pre>
     */
    org.aesh.command.option.CompletionFallback completeFallback() default org.aesh.command.option.CompletionFallback.DEFAULT;

    /**
     * Specify subcommands for this command, making it a group command.
     * When non-empty, the command acts as a group command container.
     * <p>
     * Note: {@code disableParsing} is ignored when {@code groupCommands} is non-empty.
     *
     * @return subcommand classes
     */
    Class<? extends Command>[] groupCommands() default {};
}
