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

package org.aesh.command.impl.container;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.DefaultValueProvider;
import org.aesh.command.GroupCommand;
import org.aesh.command.MethodCommand;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.container.CommandContainerBuilder;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.impl.parser.AeshCommandLineParser;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.provider.NullHelpSectionProvider;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.metadata.CommandMetadataProvider;
import org.aesh.command.metadata.MetadataProviderRegistry;
import org.aesh.command.metadata.MethodCommandMetadataProvider;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.CompletionFallback;
import org.aesh.command.option.Mixin;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionGroup;
import org.aesh.command.option.OptionList;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.CommandValidator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.util.ReflectionUtil;

/**
 * @author Aesh team
 */
@SuppressWarnings("unchecked")
public class AeshCommandContainerBuilder<CI extends CommandInvocation> implements CommandContainerBuilder<CI> {

    private boolean lazyChildResolution;
    private DefaultValueProvider defaultValueProvider;

    public void setLazyChildResolution(boolean lazyChildResolution) {
        this.lazyChildResolution = lazyChildResolution;
    }

    public void setDefaultValueProvider(DefaultValueProvider defaultValueProvider) {
        this.defaultValueProvider = defaultValueProvider;
    }

    @Override
    public CommandContainer<CI> create(Command command) throws CommandLineParserException {
        CommandMetadataProvider provider = MetadataProviderRegistry.getProvider(command.getClass());
        AeshCommandContainer<CI> container;
        if (provider != null) {
            container = buildFromProvider(provider, command);
        } else {
            container = doGenerateCommandLineParser(command);
        }
        applyDefaultValueProvider(container);
        return container;
    }

    @Override
    public CommandContainer<CI> create(Class<? extends Command> command) throws CommandLineParserException {
        CommandMetadataProvider provider = MetadataProviderRegistry.getProvider(command);
        AeshCommandContainer<CI> container;
        if (provider != null) {
            container = buildFromProvider(provider, provider.newInstance());
        } else {
            container = doGenerateCommandLineParser(ReflectionUtil.newInstance(command));
        }
        applyDefaultValueProvider(container);
        return container;
    }

    /**
     * Build a container for a {@code @MethodCommand} command identified by
     * its command name, to be invoked on the caller-supplied owner object.
     * Generated metadata is preferred: when a processor-generated provider
     * matches, the method executes through a direct call. Otherwise the
     * owner's methods are scanned reflectively as a fallback.
     *
     * @param owner the object the method is invoked on
     * @param commandName the command name or alias
     * @return a container whose command executes the method
     * @throws CommandLineParserException if no matching method serves as a command
     */
    @Override
    public CommandContainer<CI> create(Object owner, String commandName) throws CommandLineParserException {
        if (owner == null)
            throw new CommandLineParserException("Owner must not be null");
        if (commandName == null || commandName.isEmpty())
            throw new CommandLineParserException("Command name must not be empty");
        List<MethodCommandMetadataProvider> matches = new ArrayList<>();
        for (MethodCommandMetadataProvider provider : methodProvidersFor(owner.getClass())) {
            if (provider.commandName().equals(commandName))
                matches.add(provider);
            else {
                for (String alias : provider.commandAliases()) {
                    if (alias.equals(commandName)) {
                        matches.add(provider);
                        break;
                    }
                }
            }
        }
        if (matches.size() > 1)
            throw new CommandLineParserException(
                    "Ambiguous command name '" + commandName + "': matched "
                            + matches.size() + " methods");
        if (matches.size() == 1)
            return containerFromMethodProvider(matches.get(0), owner);
        return createFromMethodScan(owner, commandName);
    }

    @Override
    public CommandContainer<CI> create(Class<?> ownerClass, String commandName)
            throws CommandLineParserException {
        if (ownerClass == null)
            throw new CommandLineParserException("Owner class must not be null");
        if (commandName == null || commandName.isEmpty())
            throw new CommandLineParserException("Command name must not be empty");
        List<Method> matches = new ArrayList<>();
        for (Method candidate : ownerClass.getMethods()) {
            if (!Modifier.isStatic(candidate.getModifiers()))
                continue;
            MethodCommand command = candidate.getAnnotation(MethodCommand.class);
            if (command == null)
                continue;
            String cliName = command.name().length() < 1 ? candidate.getName() : command.name();
            if (cliName.equals(commandName)) {
                matches.add(candidate);
                continue;
            }
            for (String alias : command.aliases()) {
                if (alias.equals(commandName)) {
                    matches.add(candidate);
                    break;
                }
            }
        }
        if (matches.isEmpty())
            throw new CommandLineParserException(
                    "No static @MethodCommand named '" + commandName + "' on " + ownerClass.getName());
        if (matches.size() > 1)
            throw new CommandLineParserException(
                    "Ambiguous command name '" + commandName + "': matched "
                            + matches.size() + " methods");
        return create(null, matches.get(0));
    }

    /**
     * Build a container for a {@code @MethodCommand}-annotated method, to be
     * invoked on the caller-supplied instance. The instance is shared across
     * executions, exactly like instance-registered field commands; for static
     * methods the instance may be null and is ignored.
     * <p>
     * Validation fails fast: missing annotation, unsupported return type
     * (only {@code void}, {@code Void} and {@code CommandResult} are
     * accepted), missing instance for instance methods, and unusable
     * parameters all throw here rather than at first execution. Duplicate
     * command names across methods of one class are left to the group or
     * registry, which skip already-registered names as for classes.
     *
     * @param instance the object the method is invoked on, or null for static methods
     * @param method the {@code @MethodCommand}-annotated method
     * @return a container whose command executes the method
     * @throws CommandLineParserException if the method cannot serve as a command
     */
    @Override
    public CommandContainer<CI> create(Object instance, Method method) throws CommandLineParserException {
        if (method == null)
            throw new CommandLineParserException("Method must not be null");
        MethodCommand command = method.getAnnotation(MethodCommand.class);
        if (command == null)
            throw new CommandLineParserException(
                    "Method must be annotated with @MethodCommand: " + method);
        if (Modifier.isPrivate(method.getModifiers()))
            throw new CommandLineParserException(
                    "@MethodCommand method must not be private: use public, protected or package-private"
                            + " visibility: " + method);
        if (Modifier.isAbstract(method.getModifiers()))
            throw new CommandLineParserException(
                    "@MethodCommand method must not be abstract: " + method);
        if (instance == null && !Modifier.isStatic(method.getModifiers()))
            throw new CommandLineParserException(
                    "Non-static @MethodCommand method requires an instance: " + method);
        Class<?> returnType = method.getReturnType();
        if (returnType != void.class && returnType != Void.class
                && !CommandResult.class.isAssignableFrom(returnType))
            throw new CommandLineParserException(
                    "@MethodCommand method must return void or CommandResult: " + method);
        MethodCommandMetadataProvider generated = findMethodProvider(method);
        if (generated != null)
            return containerFromMethodProvider(generated, instance);
        method.setAccessible(true);
        String name = command.name().length() < 1 ? method.getName() : command.name();
        Object[] slots = new Object[method.getParameterCount()];
        MethodCommandAdapter<CI> adapter = new MethodCommandAdapter<>(instance, method, slots);
        ProcessedCommand<Command<CI>, CI> processedCommand = ProcessedCommandBuilder.<Command<CI>, CI> builder()
                .name(name)
                .aliases(Arrays.asList(command.aliases()))
                .description(command.description())
                .command(adapter)
                .create();
        processMethod(processedCommand, slots, method);
        validatePositionalIndexes(processedCommand);
        AeshCommandContainer<CI> container = new AeshCommandContainer<>(new AeshCommandLineParser<>(processedCommand));
        applyDefaultValueProvider(container);
        return container;
    }

    /**
     * Collect generated method-command providers for a class, walking
     * superclasses so proxied or extended owners resolve like the
     * class-command hierarchy walk does.
     */
    private static List<MethodCommandMetadataProvider> methodProvidersFor(Class<?> ownerClass) {
        List<MethodCommandMetadataProvider> result = new ArrayList<>();
        for (Class<?> current = ownerClass; current != null && current != Object.class; current = current.getSuperclass()) {
            result.addAll(MetadataProviderRegistry.getMethodProviders(current));
        }
        return result;
    }

    /**
     * Find the generated provider whose CLI name matches the given method's
     * own annotation, or null when none matches. Matching by CLI name (not
     * Java name) keeps overloads with distinct command names precise.
     */
    private static MethodCommandMetadataProvider findMethodProvider(Method method) {
        MethodCommand command = method.getAnnotation(MethodCommand.class);
        if (command == null)
            return null;
        String cliName = command.name().length() < 1 ? method.getName() : command.name();
        List<MethodCommandMetadataProvider> matches = new ArrayList<>();
        for (MethodCommandMetadataProvider provider : methodProvidersFor(method.getDeclaringClass())) {
            if (provider.commandName().equals(cliName))
                matches.add(provider);
        }
        if (matches.size() > 1)
            return null;
        return matches.isEmpty() ? null : matches.get(0);
    }

    @SuppressWarnings("unchecked")
    private AeshCommandContainer<CI> containerFromMethodProvider(MethodCommandMetadataProvider provider,
            Object owner) throws CommandLineParserException {
        ProcessedCommand<Command<CI>, CI> processedCommand = (ProcessedCommand<Command<CI>, CI>) provider
                .buildProcessedCommand(owner);
        AeshCommandContainer<CI> container = new AeshCommandContainer<>(new AeshCommandLineParser<>(processedCommand));
        applyDefaultValueProvider(container);
        return container;
    }

    /**
     * Reflective fallback for {@link #create(Object, String)} when no
     * generated provider matches: scan the owner's methods for a
     * {@code @MethodCommand} with a matching name or alias.
     */
    private CommandContainer<CI> createFromMethodScan(Object owner, String commandName)
            throws CommandLineParserException {
        List<Method> matches = new ArrayList<>();
        for (Method candidate : owner.getClass().getMethods()) {
            MethodCommand command = candidate.getAnnotation(MethodCommand.class);
            if (command == null)
                continue;
            String cliName = command.name().length() < 1 ? candidate.getName() : command.name();
            if (cliName.equals(commandName)) {
                matches.add(candidate);
                continue;
            }
            for (String alias : command.aliases()) {
                if (alias.equals(commandName)) {
                    matches.add(candidate);
                    break;
                }
            }
        }
        if (matches.isEmpty())
            throw new CommandLineParserException(
                    "No @MethodCommand named '" + commandName + "' on " + owner.getClass().getName());
        if (matches.size() > 1)
            throw new CommandLineParserException(
                    "Ambiguous command name '" + commandName + "': matched "
                            + matches.size() + " methods");
        return create(owner, matches.get(0));
    }

    @SuppressWarnings("unchecked")
    public ProcessedCommand<Command<CI>, CI> buildHelpProcessedCommand(Class<? extends Command> commandClass)
            throws CommandLineParserException {
        Class<Command<CI>> clazz = (Class<Command<CI>>) commandClass;
        CommandDefinition command = clazz.getAnnotation(CommandDefinition.class);
        if (command == null)
            throw new CommandLineParserException("Commands must be annotated with @CommandDefinition");
        boolean isGroup = command.groupCommands().length > 0 || GroupCommand.class.isAssignableFrom(clazz);
        ProcessedCommand<Command<CI>, CI> processedCommand = ProcessedCommandBuilder.<Command<CI>, CI> builder()
                .name(command.name())
                .aliases(Arrays.asList(command.aliases()))
                .description(command.description())
                .command((Command<CI>) null)
                .generateHelp(command.generateHelp())
                .disableParsing(!isGroup && command.disableParsing())
                .stopAtFirstPositional(command.stopAtFirstPositional())
                .sortOptions(command.sortOptions())
                .version(command.version())
                .helpUrl(command.helpUrl())
                .create();

        processedCommand.setCompleteFallback(command.completeFallback());
        processCommand(processedCommand, clazz, true);
        validatePositionalIndexes(processedCommand);
        if (command.helpGroup().length() > 0)
            processedCommand.setHelpGroup(command.helpGroup());
        if (command.helpSectionProvider() != NullHelpSectionProvider.class)
            processedCommand.setHelpSectionProviderClass(command.helpSectionProvider());
        return processedCommand;
    }

    private void applyDefaultValueProvider(AeshCommandContainer<CI> container) {
        if (defaultValueProvider == null || container.getParser() == null)
            return;
        ProcessedCommand<Command<CI>, CI> processedCommand = container.getParser().getProcessedCommand();
        if (processedCommand != null && processedCommand.getDefaultValueProvider() == null)
            processedCommand.setDefaultValueProvider(defaultValueProvider);
        if (container.getParser() instanceof AeshCommandLineParser)
            ((AeshCommandLineParser<CI>) container.getParser()).storeDefaultValueProvider(defaultValueProvider);
    }

    private AeshCommandContainer<CI> buildFromProvider(CommandMetadataProvider provider, Command command)
            throws CommandLineParserException {
        ProcessedCommand<Command<CI>, CI> processedCommand = provider.buildProcessedCommand(command);

        AeshCommandLineParser<CI> parser = new AeshCommandLineParser<>(processedCommand);
        AeshCommandContainer<CI> container = new AeshCommandContainer<>(parser);

        boolean isGroup = provider.isGroupCommand() || command instanceof GroupCommand;
        if (isGroup) {
            // Set child resolver so lazy children use the same builder (#517)
            setChildResolverFromBuilder(parser);
            // Add static subcommands from annotation groupCommands={...}
            if (provider.isGroupCommand()) {
                Class<? extends Command>[] childClasses = provider.groupCommandClasses();
                String[][] namesAndAliases = provider.groupCommandNamesAndAliases();
                if (namesAndAliases != null && namesAndAliases.length == childClasses.length) {
                    // Fast path: names/aliases pre-computed by the annotation processor (#575)
                    for (int i = 0; i < childClasses.length; i++) {
                        String[] entry = namesAndAliases[i];
                        container.addLazyChild(entry[0], childClasses[i]);
                        for (int j = 1; j < entry.length; j++) {
                            container.addLazyChild(entry[j], childClasses[i]);
                        }
                    }
                } else {
                    // Fallback: runtime lookup (non-generated commands, cross-module groups)
                    for (Class<? extends Command> groupClazz : childClasses) {
                        String childName = getCommandName(groupClazz);
                        if (childName != null) {
                            addLazyChildWithAliases(container, childName, groupClazz);
                        } else {
                            container.addChild(create(groupClazz));
                        }
                    }
                }
            }
            // Add dynamic subcommands from GroupCommand.getCommands() (#542)
            if (command instanceof GroupCommand) {
                List<Command<CI>> commands = ((GroupCommand<CI>) command).getCommands();
                if (commands != null) {
                    for (Command<CI> sub : commands) {
                        container.addChild(create(sub));
                    }
                }
                List<CommandContainer<CI>> parsedCommands = ((GroupCommand<CI>) command).getParsedCommands();
                if (parsedCommands != null) {
                    for (CommandContainer<CI> sub : parsedCommands) {
                        container.addChild(sub);
                    }
                }
            }
            // Eagerly resolve lazy children so the cost is paid at construction
            // time rather than on the first executeCommand() call (#577)
            if (!lazyChildResolution)
                parser.getAllChildParsers();
        }

        return container;
    }

    private AeshCommandContainer<CI> doGenerateCommandLineParser(Command<CI> commandObject) throws CommandLineParserException {
        Class<Command<CI>> clazz = (Class<Command<CI>>) commandObject.getClass();
        CommandDefinition command = clazz.getAnnotation(CommandDefinition.class);
        if (command != null) {
            boolean hasStaticGroup = command.groupCommands().length > 0;
            boolean isGroup = hasStaticGroup || commandObject instanceof GroupCommand;
            ProcessedCommand<Command<CI>, CI> processedCommand = ProcessedCommandBuilder.<Command<CI>, CI> builder()
                    .name(command.name())
                    .activator(command.activator())
                    .aliases(Arrays.asList(command.aliases()))
                    .description(command.description())
                    .validator((Class<? extends CommandValidator<Command<CI>, CI>>) command.validator())
                    .command(commandObject)
                    .resultHandler(command.resultHandler())
                    .generateHelp(command.generateHelp())
                    .disableParsing(!isGroup && command.disableParsing())
                    .stopAtFirstPositional(command.stopAtFirstPositional())
                    .sortOptions(command.sortOptions())
                    .defaultValueProvider(command.defaultValueProvider())
                    .version(command.version())
                    .helpUrl(command.helpUrl())
                    .create();

            processedCommand.setCompleteFallback(command.completeFallback());
            processCommand(processedCommand, clazz);
            validatePositionalIndexes(processedCommand);
            if (command.helpGroup().length() > 0)
                processedCommand.setHelpGroup(command.helpGroup());
            if (command.helpSectionProvider() != NullHelpSectionProvider.class)
                processedCommand.setHelpSectionProviderClass(command.helpSectionProvider());

            if (isGroup) {
                // Handle as group command
                AeshCommandLineParser<CI> groupParser = new AeshCommandLineParser<>(processedCommand);
                setChildResolverFromBuilder(groupParser);
                AeshCommandContainer<CI> groupContainer = new AeshCommandContainer<>(groupParser);

                // Add static subcommands from annotation groupCommands={...}
                if (hasStaticGroup) {
                    for (Class<? extends Command> groupClazz : command.groupCommands()) {
                        String childName = getCommandName(groupClazz);
                        if (childName != null) {
                            addLazyChildWithAliases(groupContainer, childName, groupClazz);
                        } else {
                            Command<CI> groupInstance = (Command<CI>) ReflectionUtil.newInstance(groupClazz);
                            groupContainer.addChild(doGenerateCommandLineParser(groupInstance));
                        }
                    }
                }
                // Add dynamic subcommands from GroupCommand.getCommands() (#542)
                if (commandObject instanceof GroupCommand) {
                    List<Command<CI>> commands = ((GroupCommand<CI>) commandObject).getCommands();
                    if (commands != null) {
                        for (Command<CI> sub : commands) {
                            groupContainer.addChild(doGenerateCommandLineParser(sub));
                        }
                    }
                    List<CommandContainer<CI>> parsedCommands = ((GroupCommand<CI>) commandObject).getParsedCommands();
                    if (parsedCommands != null) {
                        for (CommandContainer<CI> sub : parsedCommands) {
                            groupContainer.addChild(sub);
                        }
                    }
                }
                // Eagerly resolve lazy children so the cost is paid at construction
                // time rather than on the first executeCommand() call (#577)
                if (!lazyChildResolution)
                    groupParser.getAllChildParsers();
                return groupContainer;
            }

            return new AeshCommandContainer<>(
                    new AeshCommandLineParser<>(processedCommand));
        }

        throw new CommandLineParserException(
                "Commands must be annotated with @CommandDefinition");
    }

    private static void processCommand(ProcessedCommand processedCommand, Class clazz) throws CommandLineParserException {
        processCommand(processedCommand, clazz, false);
    }

    private static void processCommand(ProcessedCommand processedCommand, Class clazz, boolean forHelp)
            throws CommandLineParserException {
        for (Field field : ReflectionUtil.declaredFields(clazz))
            processField(processedCommand, field, forHelp);

        if (clazz.getSuperclass() != null)
            processCommand(processedCommand, clazz.getSuperclass(), forHelp);
    }

    private static void validatePositionalIndexes(ProcessedCommand processedCommand) throws CommandLineParserException {
        List<org.aesh.command.impl.internal.ProcessedOption> positional = processedCommand.getPositionalOptionsInDisplayOrder();
        for (int i = 0; i < positional.size(); i++) {
            org.aesh.command.impl.internal.ProcessedOption left = positional.get(i);
            if (!left.hasIndexRange())
                continue;
            for (int j = i + 1; j < positional.size(); j++) {
                org.aesh.command.impl.internal.ProcessedOption right = positional.get(j);
                if (!right.hasIndexRange())
                    continue;
                if (left.getIndexRange().overlaps(right.getIndexRange())) {
                    throw new CommandLineParserException(
                            "Positional index ranges overlap: "
                                    + rangeText(left) + " and " + rangeText(right)
                                    + ". Adjust index values to use distinct positional slots.");
                }
            }
        }
    }

    private static String rangeText(org.aesh.command.impl.internal.ProcessedOption option) {
        int min = option.getIndexRange().getMin();
        int max = option.getIndexRange().getMax();
        if (min == max)
            return String.valueOf(min);
        if (max == Integer.MAX_VALUE)
            return min + "..*";
        return min + ".." + max;
    }

    private static void processField(ProcessedCommand processedCommand, Field field) throws CommandLineParserException {
        processField(processedCommand, field, null, false);
    }

    /**
     * Map a {@code @MethodCommand} method's parameters to options, mirroring
     * {@link #processField(ProcessedCommand, Field, String, boolean)}. Each
     * annotated parameter becomes an option whose converted values land in
     * the adapter's slot array; {@code CommandInvocation} parameters are
     * passed straight through at execution time and need no annotation.
     */
    private static void processMethod(ProcessedCommand processedCommand, Object[] slots, Method method)
            throws CommandLineParserException {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (CommandInvocation.class.isAssignableFrom(param.getType())) {
                if (param.getAnnotation(Option.class) != null || param.getAnnotation(Argument.class) != null
                        || param.getAnnotation(OptionList.class) != null
                        || param.getAnnotation(Arguments.class) != null)
                    throw new CommandLineParserException(
                            "CommandInvocation parameter must not carry @Option/@Argument/@OptionList/@Arguments: "
                                    + param.getName() + " in " + method);
                continue;
            }
            if (param.getAnnotation(OptionGroup.class) != null || param.getAnnotation(Mixin.class) != null)
                throw new CommandLineParserException(
                        "Only @Option, @Argument, @OptionList and @Arguments are supported"
                                + " on @MethodCommand parameters: "
                                + param.getName() + " in " + method);
            Option o;
            Argument arg;
            OptionList ol;
            Arguments a;
            if ((o = param.getAnnotation(Option.class)) != null) {
                // Parameter names depend on the -parameters compiler flag,
                // so the CLI surface would change with build flags. Explicit
                // names keep it stable (and match picocli, where names= is required).
                if (o.name().length() < 1)
                    throw new CommandLineParserException(
                            "@Option on @MethodCommand parameters requires an explicit name: "
                                    + param.getName() + " in " + method);
                OptionType optionType;
                if (o.hasValue())
                    optionType = OptionType.NORMAL;
                else
                    optionType = OptionType.BOOLEAN;

                Class<?> paramType = param.getType();
                boolean isOptionalWrapped = Optional.class.isAssignableFrom(paramType);
                if (isOptionalWrapped) {
                    paramType = unwrapOptionalParameterType(param);
                }

                ProcessedOptionBuilder optionBuilder = ProcessedOptionBuilder.builder()
                        .shortName(o.shortName())
                        .name(o.name().length() < 1 ? param.getName() : o.name())
                        .description(o.description())
                        .required(o.required())
                        .valueSeparator(' ')
                        .askIfNotSet(o.askIfNotSet())
                        .acceptNameWithoutDashes(o.acceptNameWithoutDashes())
                        .selector(o.selector())
                        .addAllDefaultValues(o.defaultValue())
                        .type(paramType)
                        .fieldName(param.getName())
                        .fieldAccessor(MethodCommandAdapter.slotAccessor(slots, i))
                        .optionalWrapped(isOptionalWrapped)
                        .optionType(optionType);
                optionBuilder.converter(o.converter())
                        .completer(o.completer())
                        .validator(o.validator())
                        .activator(o.activator())
                        .renderer(o.renderer())
                        .parser(o.parser());
                processedCommand.addOption(optionBuilder
                        .overrideRequired(o.overrideRequired())
                        .optionalValue(o.optionalValue())
                        .fallbackValue("\u0000".equals(o.fallbackValue()) ? null : o.fallbackValue())
                        .negatable(o.negatable())
                        .negationPrefix(o.negationPrefix())
                        .inherited(o.inherited())
                        .descriptionUrl(o.descriptionUrl())
                        .url(o.url())
                        .completeFallback(resolveEffectiveFallback(
                                o.completeFallback(), processedCommand))
                        .aliases(o.aliases())
                        .helpGroup(o.helpGroup())
                        .exclusiveWith(o.exclusiveWith())
                        .addAllAllowedValues(o.allowedValues())
                        .visibility(o.visibility())
                        .order(o.order())
                        .build());
            } else if ((arg = param.getAnnotation(Argument.class)) != null) {
                Class<?> paramType = param.getType();
                boolean isOptionalWrapped = Optional.class.isAssignableFrom(paramType);
                if (isOptionalWrapped) {
                    paramType = unwrapOptionalParameterType(param);
                }
                if (Collection.class.isAssignableFrom(paramType))
                    throw new CommandLineParserException("Argument parameter can not be an instance of Collection");
                // Help text falls back to the parameter name when paramLabel is
                // unset, and parameter names depend on the -parameters compiler
                // flag. Explicit labels keep help output stable (like option names).
                if (arg.paramLabel().length() < 1)
                    throw new CommandLineParserException(
                            "@Argument on @MethodCommand parameters requires an explicit paramLabel: "
                                    + param.getName() + " in " + method);
                ProcessedOptionBuilder argumentBuilder = ProcessedOptionBuilder.builder()
                        .shortName('\u0000')
                        .name("")
                        .description(arg.description())
                        .required(arg.required())
                        .valueSeparator(' ')
                        .askIfNotSet(arg.askIfNotSet())
                        .selector(arg.selector())
                        .addAllDefaultValues(arg.defaultValue())
                        .type(paramType)
                        .fieldName(param.getName())
                        .fieldAccessor(MethodCommandAdapter.slotAccessor(slots, i))
                        .optionalWrapped(isOptionalWrapped)
                        .paramLabel(arg.paramLabel())
                        .arity(arg.arity())
                        .index(arg.index())
                        .optionType(OptionType.ARGUMENT);
                argumentBuilder.converter(arg.converter())
                        .completer(arg.completer())
                        .validator(arg.validator())
                        .activator(arg.activator())
                        .renderer(arg.renderer())
                        .parser(arg.parser());
                processedCommand.addArgument(argumentBuilder
                        .overrideRequired(arg.overrideRequired())
                        .inherited(arg.inherited())
                        .url(arg.url())
                        .completeFallback(resolveEffectiveFallback(
                                arg.completeFallback(), processedCommand))
                        .build());
            } else if ((ol = param.getAnnotation(OptionList.class)) != null) {
                // Parameter names depend on the -parameters compiler flag,
                // so the CLI surface would change with build flags. Explicit
                // names keep it stable (and match picocli, where names= is required).
                if (ol.name().length() < 1)
                    throw new CommandLineParserException(
                            "@OptionList on @MethodCommand parameters requires an explicit name: "
                                    + param.getName() + " in " + method);
                Class<?> rawParamType = param.getType();
                Type genericParamType = param.getParameterizedType();
                boolean listOptionalWrapped = Optional.class.isAssignableFrom(rawParamType);
                if (listOptionalWrapped && genericParamType instanceof ParameterizedType) {
                    Type innerType = ((ParameterizedType) genericParamType).getActualTypeArguments()[0];
                    if (innerType instanceof ParameterizedType) {
                        rawParamType = (Class<?>) ((ParameterizedType) innerType).getRawType();
                        genericParamType = innerType;
                    } else if (innerType instanceof Class) {
                        rawParamType = (Class<?>) innerType;
                    }
                }
                if (!Collection.class.isAssignableFrom(rawParamType))
                    throw new CommandLineParserException(
                            "OptionList parameter must be an instance of Collection: "
                                    + param.getName() + " in " + method);
                Class<?> elementType = Object.class;
                if (genericParamType instanceof ParameterizedType) {
                    Type elementArg = ((ParameterizedType) genericParamType).getActualTypeArguments()[0];
                    if (elementArg instanceof Class)
                        elementType = (Class<?>) elementArg;
                }
                ProcessedOptionBuilder optionListBuilder = ProcessedOptionBuilder.builder()
                        .shortName(ol.shortName())
                        .name(ol.name())
                        .description(ol.description())
                        .required(ol.required())
                        .valueSeparator(ol.valueSeparator())
                        .askIfNotSet(ol.askIfNotSet())
                        .acceptNameWithoutDashes(ol.acceptNameWithoutDashes())
                        .selector(ol.selector())
                        .addAllDefaultValues(ol.defaultValue())
                        .type(elementType)
                        .fieldName(param.getName())
                        .fieldAccessor(MethodCommandAdapter.slotAccessor(slots, i))
                        .optionalWrapped(listOptionalWrapped)
                        .optionType(OptionType.LIST);
                optionListBuilder.converter(ol.converter())
                        .completer(ol.completer())
                        .validator(ol.validator())
                        .activator(ol.activator())
                        .renderer(ol.renderer())
                        .parser(ol.parser());
                processedCommand.addOption(optionListBuilder
                        .overrideRequired(ol.overrideRequired())
                        .inherited(ol.inherited())
                        .descriptionUrl(ol.descriptionUrl())
                        .url(ol.url())
                        .completeFallback(resolveEffectiveFallback(
                                ol.completeFallback(), processedCommand))
                        .aliases(ol.aliases())
                        .helpGroup(ol.helpGroup())
                        .exclusiveWith(ol.exclusiveWith())
                        .addAllAllowedValues(ol.allowedValues())
                        .visibility(ol.visibility())
                        .order(ol.order())
                        .build());
            } else if ((a = param.getAnnotation(Arguments.class)) != null) {
                Class<?> rawParamType = param.getType();
                Type genericParamType = param.getParameterizedType();
                boolean argsOptionalWrapped = Optional.class.isAssignableFrom(rawParamType);
                if (argsOptionalWrapped && genericParamType instanceof ParameterizedType) {
                    Type innerType = ((ParameterizedType) genericParamType).getActualTypeArguments()[0];
                    if (innerType instanceof ParameterizedType) {
                        rawParamType = (Class<?>) ((ParameterizedType) innerType).getRawType();
                        genericParamType = innerType;
                    } else if (innerType instanceof Class) {
                        rawParamType = (Class<?>) innerType;
                    }
                }
                if (!Collection.class.isAssignableFrom(rawParamType))
                    throw new CommandLineParserException(
                            "Arguments parameter must be an instance of Collection: "
                                    + param.getName() + " in " + method);
                // Help text falls back to the parameter name when paramLabel is
                // unset, and parameter names depend on the -parameters compiler
                // flag. Explicit labels keep help output stable (like option names).
                if (a.paramLabel().length() < 1)
                    throw new CommandLineParserException(
                            "@Arguments on @MethodCommand parameters requires an explicit paramLabel: "
                                    + param.getName() + " in " + method);
                Class<?> elementType = Object.class;
                if (genericParamType instanceof ParameterizedType) {
                    Type elementArg = ((ParameterizedType) genericParamType).getActualTypeArguments()[0];
                    if (elementArg instanceof Class)
                        elementType = (Class<?>) elementArg;
                }
                ProcessedOptionBuilder argumentsBuilder = ProcessedOptionBuilder.builder()
                        .shortName('\u0000')
                        .name("")
                        .description(a.description())
                        .required(a.required())
                        .valueSeparator(a.valueSeparator())
                        .selector(a.selector())
                        .askIfNotSet(a.askIfNotSet())
                        .addAllDefaultValues(a.defaultValue())
                        .type(elementType)
                        .fieldName(param.getName())
                        .fieldAccessor(MethodCommandAdapter.slotAccessor(slots, i))
                        .optionalWrapped(argsOptionalWrapped)
                        .paramLabel(a.paramLabel())
                        .arity(a.arity())
                        .index(a.index())
                        .optionType(OptionType.ARGUMENTS);
                argumentsBuilder.converter(a.converter())
                        .completer(a.completer())
                        .validator(a.validator())
                        .activator(a.activator())
                        .parser(a.parser());
                processedCommand.setArguments(argumentsBuilder
                        .url(a.url())
                        .completeFallback(resolveEffectiveFallback(
                                a.completeFallback(), processedCommand))
                        .build());
            } else {
                throw new CommandLineParserException(
                        "Method parameter must be annotated with @Option, @Argument,"
                                + " @OptionList or @Arguments: "
                                + param.getName() + " in " + method);
            }
        }
    }

    private static Class<?> unwrapOptionalParameterType(Parameter parameter) {
        Type genericType = parameter.getParameterizedType();
        if (genericType instanceof ParameterizedType) {
            Type innerType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            if (innerType instanceof Class) {
                return (Class<?>) innerType;
            } else if (innerType instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) innerType).getRawType();
            }
        }
        return Object.class;
    }

    private static void processField(ProcessedCommand processedCommand, Field field, boolean forHelp)
            throws CommandLineParserException {
        processField(processedCommand, field, null, forHelp);
    }

    private static void processField(ProcessedCommand processedCommand, Field field, String mixinFieldName)
            throws CommandLineParserException {
        processField(processedCommand, field, mixinFieldName, false);
    }

    private static void processField(ProcessedCommand processedCommand, Field field, String mixinFieldName,
            boolean forHelp)
            throws CommandLineParserException {
        Option o;
        OptionGroup og;
        OptionList ol;
        Arguments a;
        Argument arg;
        if ((o = field.getAnnotation(Option.class)) != null) {
            OptionType optionType;
            if (o.hasValue())
                optionType = OptionType.NORMAL;
            else
                optionType = OptionType.BOOLEAN;

            // Detect Optional<T> and unwrap
            Class<?> fieldType = field.getType();
            boolean isOptionalWrapped = Optional.class.isAssignableFrom(fieldType);
            if (isOptionalWrapped) {
                fieldType = unwrapOptionalType(field);
            }

            ProcessedOptionBuilder optionBuilder = ProcessedOptionBuilder.builder()
                    .shortName(o.shortName())
                    .name(o.name().length() < 1 ? field.getName() : o.name())
                    .description(o.description())
                    .required(o.required())
                    .valueSeparator(' ')
                    .askIfNotSet(o.askIfNotSet())
                    .acceptNameWithoutDashes(o.acceptNameWithoutDashes())
                    .selector(o.selector())
                    .addAllDefaultValues(o.defaultValue())
                    .type(fieldType)
                    .fieldName(field.getName())
                    .optionalWrapped(isOptionalWrapped)
                    .optionType(optionType);
            if (!forHelp) {
                optionBuilder.converter(o.converter())
                        .completer(o.completer())
                        .validator(o.validator())
                        .activator(o.activator())
                        .renderer(o.renderer())
                        .parser(o.parser());
            }
            processedCommand.addOption(optionBuilder
                    .overrideRequired(o.overrideRequired())
                    .optionalValue(o.optionalValue())
                    .fallbackValue("\u0000".equals(o.fallbackValue()) ? null : o.fallbackValue())
                    .negatable(o.negatable())
                    .negationPrefix(o.negationPrefix())
                    .inherited(o.inherited())
                    .descriptionUrl(o.descriptionUrl())
                    .url(o.url())
                    .completeFallback(resolveEffectiveFallback(
                            o.completeFallback(), processedCommand))
                    .aliases(o.aliases())
                    .helpGroup(o.helpGroup())
                    .exclusiveWith(o.exclusiveWith())
                    .addAllAllowedValues(o.allowedValues())
                    .visibility(o.visibility())
                    .order(o.order())
                    .mixinFieldName(mixinFieldName)
                    .build());
        } else if ((ol = field.getAnnotation(OptionList.class)) != null) {
            // Support Optional<List<T>> by unwrapping Optional
            Class<?> rawFieldType = field.getType();
            Type genericType = field.getGenericType();
            boolean isOptionalWrapped = Optional.class.isAssignableFrom(rawFieldType);
            if (isOptionalWrapped && genericType instanceof ParameterizedType) {
                Type innerType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                if (innerType instanceof ParameterizedType) {
                    rawFieldType = (Class<?>) ((ParameterizedType) innerType).getRawType();
                    genericType = innerType;
                } else if (innerType instanceof Class) {
                    rawFieldType = (Class<?>) innerType;
                }
            }
            if (!Collection.class.isAssignableFrom(rawFieldType))
                throw new CommandLineParserException("OptionList field must be instance of Collection");
            Class type = Object.class;
            if (genericType instanceof ParameterizedType) {
                ParameterizedType listType = (ParameterizedType) genericType;
                type = (Class) listType.getActualTypeArguments()[0];
            }

            ProcessedOptionBuilder optionListBuilder = ProcessedOptionBuilder.builder()
                    .shortName(ol.shortName())
                    .name(ol.name().length() < 1 ? field.getName() : ol.name())
                    .description(ol.description())
                    .required(ol.required())
                    .valueSeparator(ol.valueSeparator())
                    .askIfNotSet(ol.askIfNotSet())
                    .acceptNameWithoutDashes(ol.acceptNameWithoutDashes())
                    .selector(ol.selector())
                    .addAllDefaultValues(ol.defaultValue())
                    .type(type)
                    .fieldName(field.getName())
                    .optionalWrapped(isOptionalWrapped)
                    .optionType(OptionType.LIST);
            if (!forHelp) {
                optionListBuilder.converter(ol.converter())
                        .completer(ol.completer())
                        .validator(ol.validator())
                        .activator(ol.activator())
                        .renderer(ol.renderer())
                        .parser(ol.parser());
            }
            processedCommand.addOption(optionListBuilder
                    .overrideRequired(ol.overrideRequired())
                    .inherited(ol.inherited())
                    .descriptionUrl(ol.descriptionUrl())
                    .url(ol.url())
                    .completeFallback(resolveEffectiveFallback(
                            ol.completeFallback(), processedCommand))
                    .aliases(ol.aliases())
                    .helpGroup(ol.helpGroup())
                    .exclusiveWith(ol.exclusiveWith())
                    .addAllAllowedValues(ol.allowedValues())
                    .visibility(ol.visibility())
                    .order(ol.order())
                    .mixinFieldName(mixinFieldName)
                    .build());

        } else if ((og = field.getAnnotation(OptionGroup.class)) != null) {
            // Support Optional<Map<K,V>> by unwrapping Optional
            Class<?> ogRawType = field.getType();
            Type ogGenericType = field.getGenericType();
            boolean ogOptionalWrapped = Optional.class.isAssignableFrom(ogRawType);
            if (ogOptionalWrapped && ogGenericType instanceof ParameterizedType) {
                Type innerType = ((ParameterizedType) ogGenericType).getActualTypeArguments()[0];
                if (innerType instanceof ParameterizedType) {
                    ogRawType = (Class<?>) ((ParameterizedType) innerType).getRawType();
                    ogGenericType = innerType;
                } else if (innerType instanceof Class) {
                    ogRawType = (Class<?>) innerType;
                }
            }
            if (!Map.class.isAssignableFrom(ogRawType))
                throw new CommandLineParserException("OptionGroup field must be instance of Map");
            Class type = Object.class;
            if (ogGenericType instanceof ParameterizedType) {
                ParameterizedType listType = (ParameterizedType) ogGenericType;
                type = (Class) listType.getActualTypeArguments()[1];
            }

            ProcessedOptionBuilder optionGroupBuilder = ProcessedOptionBuilder.builder()
                    .shortName(og.shortName())
                    .name(og.name().length() < 1
                            ? (og.shortName() != '\u0000' ? "" : field.getName())
                            : og.name())
                    .optionalWrapped(ogOptionalWrapped)
                    .description(og.description())
                    .required(og.required())
                    .valueSeparator(',')
                    .askIfNotSet(og.askIfNotSet())
                    .acceptNameWithoutDashes(og.acceptNameWithoutDashes())
                    .selector(og.selector())
                    .addAllDefaultValues(og.defaultValue())
                    .type(type)
                    .fieldName(field.getName())
                    .optionType(OptionType.GROUP);
            if (!forHelp) {
                optionGroupBuilder.converter(og.converter())
                        .completer(og.completer())
                        .validator(og.validator())
                        .activator(og.activator())
                        .renderer(og.renderer())
                        .parser(og.parser());
            }
            processedCommand.addOption(optionGroupBuilder
                    .overrideRequired(og.overrideRequired())
                    .inherited(og.inherited())
                    .descriptionUrl(og.descriptionUrl())
                    .url(og.url())
                    .completeFallback(resolveEffectiveFallback(
                            og.completeFallback(), processedCommand))
                    .aliases(og.aliases())
                    .helpGroup(og.helpGroup())
                    .exclusiveWith(og.exclusiveWith())
                    .addAllAllowedValues(og.allowedValues())
                    .visibility(og.visibility())
                    .order(og.order())
                    .mixinFieldName(mixinFieldName)
                    .build());
        }

        else if ((a = field.getAnnotation(Arguments.class)) != null) {
            // Support Optional<List<T>> by unwrapping Optional
            Class<?> argsRawType = field.getType();
            Type argsGenericType = field.getGenericType();
            boolean argsOptionalWrapped = Optional.class.isAssignableFrom(argsRawType);
            if (argsOptionalWrapped && argsGenericType instanceof ParameterizedType) {
                Type innerType = ((ParameterizedType) argsGenericType).getActualTypeArguments()[0];
                if (innerType instanceof ParameterizedType) {
                    argsRawType = (Class<?>) ((ParameterizedType) innerType).getRawType();
                    argsGenericType = innerType;
                } else if (innerType instanceof Class) {
                    argsRawType = (Class<?>) innerType;
                }
            }
            if (!Collection.class.isAssignableFrom(argsRawType))
                throw new CommandLineParserException("Arguments field must be instance of Collection");
            Class type = Object.class;
            if (argsGenericType instanceof ParameterizedType) {
                ParameterizedType listType = (ParameterizedType) argsGenericType;
                type = (Class) listType.getActualTypeArguments()[0];
            }
            ProcessedOptionBuilder argumentsBuilder = ProcessedOptionBuilder.builder()
                    .shortName('\u0000')
                    .name("")
                    .description(a.description())
                    .required(a.required())
                    .valueSeparator(a.valueSeparator())
                    .selector(a.selector())
                    .askIfNotSet(a.askIfNotSet())
                    .addAllDefaultValues(a.defaultValue())
                    .type(type)
                    .fieldName(field.getName())
                    .optionalWrapped(argsOptionalWrapped)
                    .paramLabel(a.paramLabel())
                    .arity(a.arity())
                    .index(a.index())
                    .optionType(OptionType.ARGUMENTS);
            if (!forHelp) {
                argumentsBuilder.converter(a.converter())
                        .completer(a.completer())
                        .validator(a.validator())
                        .activator(a.activator())
                        .parser(a.parser());
            }
            processedCommand.setArguments(argumentsBuilder
                    .url(a.url())
                    .completeFallback(resolveEffectiveFallback(
                            a.completeFallback(), processedCommand))
                    .mixinFieldName(mixinFieldName)
                    .build());
        } else if ((arg = field.getAnnotation(Argument.class)) != null) {
            Class<?> argFieldType = field.getType();
            boolean argOptionalWrapped = Optional.class.isAssignableFrom(argFieldType);
            if (argOptionalWrapped) {
                argFieldType = unwrapOptionalType(field);
            }
            if (Collection.class.isAssignableFrom(argFieldType))
                throw new CommandLineParserException("Argument field can not be an instance of Collection");
            OptionType optionType = OptionType.ARGUMENT;
            ProcessedOptionBuilder argumentBuilder = ProcessedOptionBuilder.builder()
                    .shortName('\u0000')
                    .name("")
                    .description(arg.description())
                    .required(arg.required())
                    .valueSeparator(' ')
                    .askIfNotSet(arg.askIfNotSet())
                    .selector(arg.selector())
                    .addAllDefaultValues(arg.defaultValue())
                    .type(argFieldType)
                    .fieldName(field.getName())
                    .optionalWrapped(argOptionalWrapped)
                    .paramLabel(arg.paramLabel())
                    .arity(arg.arity())
                    .index(arg.index())
                    .optionType(optionType);
            if (!forHelp) {
                argumentBuilder.converter(arg.converter())
                        .completer(arg.completer())
                        .validator(arg.validator())
                        .activator(arg.activator())
                        .renderer(arg.renderer())
                        .parser(arg.parser());
            }
            processedCommand.addArgument(argumentBuilder
                    .overrideRequired(arg.overrideRequired())
                    .inherited(arg.inherited())
                    .url(arg.url())
                    .completeFallback(resolveEffectiveFallback(
                            arg.completeFallback(), processedCommand))
                    .mixinFieldName(mixinFieldName)
                    .build());
        } else if (field.getAnnotation(Mixin.class) != null) {
            // Chain mixinFieldName for nested mixins: "outer" + "inner" -> "outer.inner"
            String nestedMixinName = mixinFieldName != null
                    ? mixinFieldName + "." + field.getName()
                    : field.getName();
            processMixinClass(processedCommand, field.getType(), nestedMixinName, forHelp);
        }
    }

    private static void processMixinField(ProcessedCommand processedCommand, Field mixinField)
            throws CommandLineParserException {
        processMixinClass(processedCommand, mixinField.getType(), mixinField.getName(), false);
    }

    private static void processMixinClass(ProcessedCommand processedCommand, Class<?> clazz, String mixinFieldName)
            throws CommandLineParserException {
        processMixinClass(processedCommand, clazz, mixinFieldName, false);
    }

    private static void processMixinClass(ProcessedCommand processedCommand, Class<?> clazz, String mixinFieldName,
            boolean forHelp)
            throws CommandLineParserException {
        for (Field field : ReflectionUtil.declaredFields(clazz)) {
            processField(processedCommand, field, mixinFieldName, forHelp);
        }
        if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
            processMixinClass(processedCommand, clazz.getSuperclass(), mixinFieldName, forHelp);
        }
    }

    /** Cached child resolver function to avoid repeated lambda allocation. */
    private Function<Class<? extends Command>, CommandContainer<CI>> cachedChildResolver;

    /**
     * Set the child resolver on a parser, using a cached function that
     * delegates to this builder. The function is created once per builder
     * instance and reused for all parsers.
     */
    private void setChildResolverFromBuilder(AeshCommandLineParser<CI> parser) {
        if (cachedChildResolver == null) {
            cachedChildResolver = clazz -> {
                try {
                    return this.create(clazz);
                } catch (CommandLineParserException e) {
                    throw new RuntimeException(e);
                }
            };
        }
        parser.setChildResolver(cachedChildResolver);
    }

    /**
     * Register a lazy child by its primary name and also by its aliases,
     * so alias resolution works via simple map lookup at parse time.
     */
    private static <CI extends CommandInvocation> void addLazyChildWithAliases(
            AeshCommandContainer<CI> container, String childName,
            Class<? extends Command> clazz) throws CommandLineParserException {
        container.addLazyChild(childName, clazz);
        String[] aliases = getCommandAliases(clazz);
        if (aliases != null) {
            for (String alias : aliases) {
                container.addLazyChild(alias, clazz);
            }
        }
    }

    /**
     * Resolve the effective CompletionFallback for an option/argument.
     * If the option-level value is DEFAULT, use the command-level value instead.
     * If both are DEFAULT, the type-based heuristic in ProcessedOptionBuilder.build()
     * will apply as usual.
     */
    private static CompletionFallback resolveEffectiveFallback(
            CompletionFallback optionLevel,
            ProcessedCommand<?, ?> processedCommand) {
        if (optionLevel != CompletionFallback.DEFAULT)
            return optionLevel;
        return processedCommand.getCompleteFallback();
    }

    @SuppressWarnings("unchecked")
    private static String[] getCommandAliases(Class<? extends Command> clazz) {
        // Check metadata provider first to avoid reflection on the processor path
        CommandMetadataProvider provider = MetadataProviderRegistry.getProvider(clazz);
        if (provider != null) {
            String[] aliases = provider.commandAliases();
            return aliases != null && aliases.length > 0 ? aliases : null;
        }
        // Fallback to annotation reflection for non-processed commands
        CommandDefinition cd = clazz.getAnnotation(CommandDefinition.class);
        if (cd != null && cd.aliases().length > 0)
            return cd.aliases();
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String getCommandName(Class<? extends Command> clazz) {
        CommandMetadataProvider provider = MetadataProviderRegistry.getProvider(clazz);
        if (provider != null) {
            return provider.commandName();
        }
        CommandDefinition cd = clazz.getAnnotation(CommandDefinition.class);
        if (cd != null)
            return cd.name();
        return null;
    }

    /**
     * Unwrap Optional&lt;T&gt; to get T from a field's generic type.
     */
    private static Class<?> unwrapOptionalType(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            Type innerType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            if (innerType instanceof Class) {
                return (Class<?>) innerType;
            } else if (innerType instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) innerType).getRawType();
            }
        }
        return Object.class;
    }

    public static void parseAndPopulate(Command<CommandInvocation> instance, String input)
            throws CommandLineParserException, OptionValidatorException {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        CommandLineParser<CommandInvocation> cl = builder.doGenerateCommandLineParser(instance).getParser();
        InvocationProviders invocationProviders = new AeshInvocationProviders();
        cl.parse(input);
        cl.getCommandPopulator().populateObject(cl.getProcessedCommand(), invocationProviders, null,
                CommandLineParser.Mode.VALIDATE);
    }

}
