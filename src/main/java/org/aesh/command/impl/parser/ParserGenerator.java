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
package org.aesh.command.impl.parser;

import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.option.Option;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.impl.activator.AeshCommandActivatorProvider;
import org.aesh.command.impl.validator.AeshValidatorInvocationProvider;
import org.aesh.command.option.Arguments;
import org.aesh.command.CommandDefinition;
import org.aesh.command.option.OptionGroup;
import org.aesh.command.option.OptionList;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.Command;
import org.aesh.command.impl.activator.AeshOptionActivatorProvider;
import org.aesh.command.impl.completer.AeshCompleterInvocationProvider;
import org.aesh.command.impl.container.AeshCommandContainer;
import org.aesh.command.impl.converter.AeshConverterInvocationProvider;
import org.aesh.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.aesh.command.GroupCommand;

/**
 * Generates a {@link AeshCommandLineParser} based on annotations defined in
 * the specified class.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParserGenerator {

    public static AeshCommandContainer generateCommandLineParser(Command paramInstance) throws CommandLineParserException {
        return doGenerateCommandLineParser(paramInstance);
    }

    public static AeshCommandContainer generateCommandLineParser(Class<? extends Command> clazz) throws CommandLineParserException {
        return doGenerateCommandLineParser(ReflectionUtil.newInstance(clazz));
    }

    private static AeshCommandContainer doGenerateCommandLineParser(Command commandObject) throws CommandLineParserException {
        Class clazz = commandObject.getClass();
        CommandDefinition command = (CommandDefinition) clazz.getAnnotation(CommandDefinition.class);
        if(command != null) {
            ProcessedCommand processedCommand = new ProcessedCommandBuilder()
                    .name(command.name())
                    .activator(command.activator())
                    .aliases(Arrays.asList(command.aliases()))
                    .description(command.description())
                    .validator(command.validator())
                    .command(commandObject)
                    .resultHandler(command.resultHandler())
                    .create();

            processCommand(processedCommand, clazz);

            return new AeshCommandContainer(
                    new CommandLineParserBuilder()
                            .processedCommand(processedCommand)
                            .create());
        }

        GroupCommandDefinition groupCommand = (GroupCommandDefinition) clazz.getAnnotation(GroupCommandDefinition.class);
        if(groupCommand != null) {
            ProcessedCommand processedGroupCommand = new ProcessedCommandBuilder()
                    .name(groupCommand.name())
                    .activator(groupCommand.activator())
                    .aliases(Arrays.asList(groupCommand.aliases()))
                    .description(groupCommand.description())
                    .validator(groupCommand.validator())
                    .command((Command) commandObject)
                    .resultHandler(groupCommand.resultHandler())
                    .create();

            processCommand(processedGroupCommand, clazz);

            AeshCommandContainer groupContainer;

            groupContainer = new AeshCommandContainer(
                    new CommandLineParserBuilder()
                            .processedCommand(processedGroupCommand)
                            .create());

            if (commandObject instanceof GroupCommand) {
                List<Command> commands = ((GroupCommand) commandObject).getCommands();
                if (commands != null) {
                    for (Command sub : commands) {
                        groupContainer.addChild(ParserGenerator.doGenerateCommandLineParser(sub));
                    }
                }
            } else {
                for (Class<? extends Command> groupClazz : groupCommand.groupCommands()) {
                    Command groupInstance = ReflectionUtil.newInstance(groupClazz);
                    groupContainer.addChild(ParserGenerator.doGenerateCommandLineParser(groupInstance));
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
        if((o = field.getAnnotation(Option.class)) != null) {
            OptionType optionType;
            if(o.hasValue())
                optionType = OptionType.NORMAL;
            else
                optionType = OptionType.BOOLEAN;
            if(o.name() == null || o.name().length() < 1) {
                processedCommand.addOption(
                            /*
                            o.shortName(), field.getName(), o.description(),
                            o.argument(), o.required(), ',', o.defaultValue(),
                            field.getType(), field.getName(), optionType, o.converter(),
                            o.completer(), o.validator(), o.activator(), o.renderer(), o.overrideRequired());
                            */

                        new ProcessedOptionBuilder()
                                .shortName(o.shortName())
                                .name(field.getName())
                                .description(o.description())
                                .required(o.required())
                                .valueSeparator(',')
                                .addAllDefaultValues(o.defaultValue())
                                .type(field.getType())
                                .fieldName(field.getName())
                                .optionType(optionType)
                                .converter(o.converter())
                                .completer(o.completer())
                                .validator(o.validator())
                                .activator(o.activator())
                                .renderer(o.renderer())
                                .overrideRequired(o.overrideRequired())
                                .create()
                );

            }
            else {
                processedCommand.addOption(
                        //o.shortName(), o.name(), o.description(),
                        //o.argument(), o.required(), ',', o.defaultValue(),
                        //field.getType(), field.getName(), optionType, o.converter(),
                        //o.completer(), o.validator(), o.activator(), o.renderer(), o.overrideRequired());
                        new ProcessedOptionBuilder()
                                .shortName(o.shortName())
                                .name(o.name())
                                .description(o.description())
                                .required(o.required())
                                .valueSeparator(',')
                                .addAllDefaultValues(o.defaultValue())
                                .type(field.getType())
                                .fieldName(field.getName())
                                .optionType(optionType)
                                .converter(o.converter())
                                .completer(o.completer())
                                .validator(o.validator())
                                .activator(o.activator())
                                .renderer(o.renderer())
                                .overrideRequired(o.overrideRequired())
                                .create());

            }

        }
        else if((ol = field.getAnnotation(OptionList.class)) != null) {
            if(!Collection.class.isAssignableFrom(field.getType()))
                throw new CommandLineParserException("OptionList field must be instance of Collection");
            Class type = Object.class;
            if(field.getGenericType() != null) {
                ParameterizedType listType = (ParameterizedType) field.getGenericType();
                type = (Class) listType.getActualTypeArguments()[0];
            }
            if(ol.name() == null || ol.name().length() < 1) {
                processedCommand.addOption(
                        new ProcessedOptionBuilder()
                                .shortName(ol.shortName())
                                .name(field.getName())
                                .description(ol.description())
                                .required(ol.required())
                                .valueSeparator(ol.valueSeparator())
                                .addAllDefaultValues(ol.defaultValue())
                                .type(type)
                                .fieldName(field.getName())
                                .optionType(OptionType.LIST)
                                .converter(ol.converter())
                                .completer(ol.completer())
                                .validator(ol.validator())
                                .activator(ol.activator())
                                .renderer(ol.renderer()).create());

            }
            else {
                processedCommand.addOption(
                        new ProcessedOptionBuilder()
                                .shortName(ol.shortName())
                                .name(ol.name())
                                .description(ol.description())
                                .required(ol.required())
                                .valueSeparator(ol.valueSeparator())
                                .addAllDefaultValues(ol.defaultValue())
                                .type(type)
                                .fieldName(field.getName())
                                .optionType(OptionType.LIST)
                                .converter(ol.converter())
                                .completer(ol.completer())
                                .validator(ol.validator())
                                .activator(ol.activator())
                                .renderer(ol.renderer()).create());

            }
        }
        else if((og = field.getAnnotation(OptionGroup.class)) != null) {
            if(!Map.class.isAssignableFrom(field.getType()))
                throw new CommandLineParserException("OptionGroup field must be instance of Map");
            Class type = Object.class;
            if(field.getGenericType() != null) {
                ParameterizedType listType = (ParameterizedType) field.getGenericType();
                type = (Class) listType.getActualTypeArguments()[1];
            }
            String name;
            if(og.name() == null || og.name().length() < 1)
                name = field.getName();
            else
                name = og.name();

            processedCommand.addOption( new ProcessedOptionBuilder()
                    .shortName(og.shortName())
                    .name(name)
                    .description(og.description())
                    .required(og.required())
                    .valueSeparator(',')
                    .addAllDefaultValues(og.defaultValue())
                    .type(type)
                    .fieldName(field.getName())
                    .optionType(OptionType.GROUP)
                    .converter(og.converter())
                    .completer(og.completer())
                    .validator(og.validator())
                    .activator(og.activator())
                    .renderer(og.renderer())
                    .create());
        }

        else if((a = field.getAnnotation(Arguments.class)) != null) {
            if(!Collection.class.isAssignableFrom(field.getType()))
                throw new CommandLineParserException("Arguments field must be instance of Collection");
            Class type = Object.class;
            if(field.getGenericType() != null) {
                ParameterizedType listType = (ParameterizedType) field.getGenericType();
                type = (Class) listType.getActualTypeArguments()[0];
            }
            processedCommand.setArgument( new ProcessedOptionBuilder()
                    .shortName('\u0000')
                    .name("")
                    .description(a.description())
                    .required(false)
                    .valueSeparator(a.valueSeparator())
                    .addAllDefaultValues(a.defaultValue())
                    .type(type)
                    .fieldName(field.getName())
                    .optionType(OptionType.ARGUMENT)
                    .converter(a.converter())
                    .completer(a.completer())
                    .validator(a.validator())
                    .activator(a.activator())
                    .create());
        }
    }

    public static void parseAndPopulate(Command instance, String commandName, String... input)
            throws CommandLineParserException, OptionValidatorException {
        StringBuilder builder = new StringBuilder(commandName);
        for(String s : input)
            builder.append(" ").append(s);

        parseAndPopulate(instance, builder.toString());
    }

   public static void parseAndPopulate(Command instance, String input) throws CommandLineParserException, OptionValidatorException {
        CommandLineParser cl = doGenerateCommandLineParser(instance).getParser();
        InvocationProviders invocationProviders = new AeshInvocationProviders(
                new AeshConverterInvocationProvider(),
                new AeshCompleterInvocationProvider(),
                new AeshValidatorInvocationProvider(),
                new AeshOptionActivatorProvider(),
                new AeshCommandActivatorProvider());
        cl.getCommandPopulator().populateObject( cl.parse(input), invocationProviders, null, true);
    }

}
