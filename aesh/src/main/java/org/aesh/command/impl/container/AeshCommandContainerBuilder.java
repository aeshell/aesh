/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
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

import org.aesh.command.CommandDefinition;
import org.aesh.command.GroupCommand;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.container.CommandContainerBuilder;
import org.aesh.command.impl.activator.AeshCommandActivatorProvider;
import org.aesh.command.impl.activator.AeshOptionActivatorProvider;
import org.aesh.command.impl.completer.AeshCompleterInvocationProvider;
import org.aesh.command.impl.converter.AeshConverterInvocationProvider;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.parser.CommandLineParserBuilder;
import org.aesh.command.Command;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.impl.validator.AeshValidatorInvocationProvider;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionGroup;
import org.aesh.command.option.OptionList;
import org.aesh.command.validator.CommandValidator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@SuppressWarnings("unchecked")
public class AeshCommandContainerBuilder<CI extends CommandInvocation> implements CommandContainerBuilder<CI> {

    @Override
    public CommandContainer<CI> create(Command command) throws CommandLineParserException {
        return doGenerateCommandLineParser(command);
    }

    @Override
    public CommandContainer<CI> create(Class<? extends Command> command) throws CommandLineParserException {
        return doGenerateCommandLineParser(ReflectionUtil.newInstance(command));
    }

    private AeshCommandContainer<CI> doGenerateCommandLineParser(Command<CI> commandObject) throws CommandLineParserException {
        Class<Command<CI>> clazz = (Class<Command<CI>>) commandObject.getClass();
        CommandDefinition command = clazz.getAnnotation(CommandDefinition.class);
        if(command != null) {
            ProcessedCommand<Command<CI>, CI> processedCommand = ProcessedCommandBuilder.<Command<CI>, CI>builder()
                    .name(command.name())
                    .activator(command.activator())
                    .aliases(Arrays.asList(command.aliases()))
                    .description(command.description())
                    .validator((Class<? extends CommandValidator<Command<CI>, CI>>) command.validator())
                    .command(commandObject)
                    .resultHandler(command.resultHandler())
                    .generateHelp(command.generateHelp())
                    .disableParsing(command.disableParsing())
                    .version(command.version())
                    .create();

            processCommand(processedCommand, clazz);

            return new AeshCommandContainer<>(
                    CommandLineParserBuilder.<Command<CI>, CI>builder()
                            .processedCommand(processedCommand)
                            .create());
        }

        GroupCommandDefinition groupCommand = clazz.getAnnotation(GroupCommandDefinition.class);
        if(groupCommand != null) {
            ProcessedCommand<Command<CI>,CI> processedGroupCommand = ProcessedCommandBuilder.<Command<CI>, CI>builder()
                    .name(groupCommand.name())
                    .activator(groupCommand.activator())
                    .aliases(Arrays.asList(groupCommand.aliases()))
                    .description(groupCommand.description())
                    .validator((Class<? extends CommandValidator<Command<CI>, CI>>) groupCommand.validator())
                    .command(commandObject)
                    .generateHelp(groupCommand.generateHelp())
                    .version(groupCommand.version())
                    .resultHandler(groupCommand.resultHandler())
                    .create();

            processCommand(processedGroupCommand, clazz);

            AeshCommandContainer<CI> groupContainer;

            groupContainer = new AeshCommandContainer<>(
                    CommandLineParserBuilder.<Command<CI>, CI>builder()
                            .processedCommand(processedGroupCommand)
                            .create());

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
            } else {
                for (Class<? extends Command> groupClazz : groupCommand.groupCommands()) {
                    Command<CI> groupInstance = (Command<CI>) ReflectionUtil.newInstance(groupClazz);
                    groupContainer.addChild(doGenerateCommandLineParser(groupInstance));
                }
            }

            return groupContainer;
        }
        else
            throw new CommandLineParserException("Commands must be annotated with @CommandDefinition or @GroupCommandDefinition");
    }

    private static void processCommand(ProcessedCommand processedCommand, Class clazz) throws CommandLineParserException {
        for(Field field : clazz.getDeclaredFields())
            processField(processedCommand, field);

        if(clazz.getSuperclass() != null)
            processCommand(processedCommand, clazz.getSuperclass());
    }

    private static void processField(ProcessedCommand processedCommand, Field field) throws CommandLineParserException {
        Option o;
        OptionGroup og;
        OptionList ol;
        Arguments a;
        Argument arg;
        if((o = field.getAnnotation(Option.class)) != null) {
            OptionType optionType;
            if(o.hasValue())
                optionType = OptionType.NORMAL;
            else
                optionType = OptionType.BOOLEAN;

            processedCommand.addOption(
                    ProcessedOptionBuilder.builder()
                            .shortName(o.shortName())
                            .name(o.name().length() < 1 ? field.getName() : o.name())
                            .description(o.description())
                            .required(o.required())
                            .valueSeparator(' ')
                            .askIfNotSet(o.askIfNotSet())
                            .selector(o.selector())
                            .addAllDefaultValues(o.defaultValue())
                            .type(field.getType())
                            .fieldName(field.getName())
                            .optionType(optionType)
                            .converter(o.converter())
                            .completer(o.completer())
                            .validator(o.validator())
                            .activator(o.activator())
                            .renderer(o.renderer())
                            .parser(o.parser())
                            .overrideRequired(o.overrideRequired())
                            .build()
            );
        }
        else if((ol = field.getAnnotation(OptionList.class)) != null) {
            if(!Collection.class.isAssignableFrom(field.getType()))
                throw new CommandLineParserException("OptionList field must be instance of Collection");
            Class type = Object.class;
            if(field.getGenericType() != null) {
                ParameterizedType listType = (ParameterizedType) field.getGenericType();
                type = (Class) listType.getActualTypeArguments()[0];
            }

            processedCommand.addOption(
                    ProcessedOptionBuilder.builder()
                            .shortName(ol.shortName())
                            .name(ol.name().length() < 1 ? field.getName() : ol.name())
                            .description(ol.description())
                            .required(ol.required())
                            .valueSeparator(ol.valueSeparator())
                            .askIfNotSet(ol.askIfNotSet())
                            .selector(ol.selector())
                            .addAllDefaultValues(ol.defaultValue())
                            .type(type)
                            .fieldName(field.getName())
                            .optionType(OptionType.LIST)
                            .converter(ol.converter())
                            .completer(ol.completer())
                            .validator(ol.validator())
                            .activator(ol.activator())
                            .renderer(ol.renderer())
                            .parser(ol.parser())
                            .build());

        }
        else if((og = field.getAnnotation(OptionGroup.class)) != null) {
            if(!Map.class.isAssignableFrom(field.getType()))
                throw new CommandLineParserException("OptionGroup field must be instance of Map");
            Class type = Object.class;
            if(field.getGenericType() != null) {
                ParameterizedType listType = (ParameterizedType) field.getGenericType();
                type = (Class) listType.getActualTypeArguments()[1];
            }

            processedCommand.addOption( ProcessedOptionBuilder.builder()
                    .shortName(og.shortName())
                    .name( og.name().length() < 1 ? field.getName() : og.name())
                    .description(og.description())
                    .required(og.required())
                    .valueSeparator(',')
                    .askIfNotSet(og.askIfNotSet())
                    .addAllDefaultValues(og.defaultValue())
                    .type(type)
                    .fieldName(field.getName())
                    .optionType(OptionType.GROUP)
                    .converter(og.converter())
                    .completer(og.completer())
                    .validator(og.validator())
                    .activator(og.activator())
                    .renderer(og.renderer())
                    .parser(og.parser())
                    .build());
        }

        else if((a = field.getAnnotation(Arguments.class)) != null) {
            if(!Collection.class.isAssignableFrom(field.getType()))
                throw new CommandLineParserException("Arguments field must be instance of Collection");
            if(processedCommand.getArgument() != null)
                throw new CommandLineParserException("Arguments can not be defined with an Argument type");
            Class type = Object.class;
            if(field.getGenericType() != null) {
                ParameterizedType listType = (ParameterizedType) field.getGenericType();
                type = (Class) listType.getActualTypeArguments()[0];
            }
            processedCommand.setArguments( ProcessedOptionBuilder.builder()
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
                    .optionType(OptionType.ARGUMENTS)
                    .converter(a.converter())
                    .completer(a.completer())
                    .validator(a.validator())
                    .activator(a.activator())
                    .parser(a.parser())
                    .build());
        }
        else if((arg = field.getAnnotation(Argument.class)) != null) {
            if(processedCommand.getArgument() != null)
                throw new CommandLineParserException("Argument can not be defined more than once pr class");
            if(processedCommand.getArguments() != null)
                throw new CommandLineParserException("Argument can not be defined with an Arguments type");
            if(Collection.class.isAssignableFrom(field.getType()))
                throw new CommandLineParserException("Argument field can not be an instance of Collection");
             OptionType optionType = OptionType.ARGUMENT;
            processedCommand.setArgument(
                    ProcessedOptionBuilder.builder()
                            .shortName('\u0000')
                            .name("")
                            .description(arg.description())
                            .required(arg.required())
                            .valueSeparator(' ')
                            .askIfNotSet(arg.askIfNotSet())
                            .selector(arg.selector())
                            .addAllDefaultValues(arg.defaultValue())
                            .type(field.getType())
                            .fieldName(field.getName())
                            .optionType(optionType)
                            .converter(arg.converter())
                            .completer(arg.completer())
                            .validator(arg.validator())
                            .activator(arg.activator())
                            .renderer(arg.renderer())
                            .parser(arg.parser())
                            .overrideRequired(arg.overrideRequired())
                            .build()
            );
        }
    }

   public static void parseAndPopulate(Command<CommandInvocation> instance, String input) throws CommandLineParserException, OptionValidatorException {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        CommandLineParser<CommandInvocation> cl = builder.doGenerateCommandLineParser(instance).getParser();
        InvocationProviders invocationProviders = new AeshInvocationProviders(
                new AeshConverterInvocationProvider(),
                new AeshCompleterInvocationProvider(),
                new AeshValidatorInvocationProvider(),
                new AeshOptionActivatorProvider(),
                new AeshCommandActivatorProvider());
        cl.parse(input);
        cl.getCommandPopulator().populateObject( cl.getProcessedCommand(), invocationProviders, null, CommandLineParser.Mode.VALIDATE);
    }

}
