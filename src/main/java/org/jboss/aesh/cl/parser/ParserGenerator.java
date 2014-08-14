/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.parser;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.OptionGroup;
import org.jboss.aesh.cl.OptionList;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.ProcessedCommandBuilder;
import org.jboss.aesh.cl.internal.OptionType;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedOptionBuilder;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshInvocationProviders;
import org.jboss.aesh.console.InvocationProviders;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.activator.AeshOptionActivatorProvider;
import org.jboss.aesh.console.command.completer.AeshCompleterInvocationProvider;
import org.jboss.aesh.console.command.container.AeshCommandContainer;
import org.jboss.aesh.console.command.converter.AeshConverterInvocationProvider;
import org.jboss.aesh.console.command.validator.AeshValidatorInvocationProvider;
import org.jboss.aesh.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Map;

/**
 * Generates a {@link AeshCommandLineParser} based on annotations defined in
 * the specified class.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParserGenerator {

    public static AeshCommandContainer generateCommandLineParser(Object paramInstance) throws CommandLineParserException {
        if(paramInstance instanceof Command)
            return doGenerateCommandLineParser(paramInstance, true, false);
        else
            return doGenerateCommandLineParser(paramInstance, false, false);
    }

    public static AeshCommandContainer generateCommandLineParser(Class clazz) throws CommandLineParserException {
        for(Class iName : clazz.getInterfaces()) {
            if(iName.equals(Command.class))
                return doGenerateCommandLineParser(ReflectionUtil.newInstance(clazz), true, false);
        }

        return doGenerateCommandLineParser(ReflectionUtil.newInstance(clazz), false, false);
    }

    private static AeshCommandContainer doGenerateCommandLineParser(Object commandObject,
                                                                    boolean clazzIsaCommand, boolean isChild) throws CommandLineParserException {
        Class clazz = commandObject.getClass();
        CommandDefinition command = (CommandDefinition) clazz.getAnnotation(CommandDefinition.class);
        if(command != null) {
            ProcessedCommand processedCommand = new ProcessedCommandBuilder()
                    .name(command.name())
                    .description(command.description())
                    .validator(command.validator())
                    .resultHandler(command.resultHandler()).create();

            processCommand(processedCommand, clazz);

            if(clazzIsaCommand)
                return new AeshCommandContainer(
                        new CommandLineParserBuilder()
                                .processedCommand(processedCommand)
                                .command((Command) commandObject)
                                .create());
            else
                return new AeshCommandContainer(
                        new CommandLineParserBuilder()
                                .processedCommand(processedCommand)
                                .create());
        }
        GroupCommandDefinition groupCommand = (GroupCommandDefinition) clazz.getAnnotation(GroupCommandDefinition.class);
        if(groupCommand != null) {
            ProcessedCommand processedGroupCommand = new ProcessedCommandBuilder()
                    .name(groupCommand.name())
                    .description(groupCommand.description())
                    .validator(groupCommand.validator())
                    .resultHandler(groupCommand.resultHandler())
                    .create();

            AeshCommandContainer groupContainer;
            if(clazzIsaCommand)
                groupContainer = new AeshCommandContainer(
                        new CommandLineParserBuilder()
                                .processedCommand(processedGroupCommand)
                                .command((Command) commandObject)
                                .create());
            else
                groupContainer = new AeshCommandContainer(
                        new CommandLineParserBuilder()
                                .processedCommand(processedGroupCommand)
                                .create());

            for(Class groupClazz : groupCommand.groupCommands()) {
                Object groupInstance = ReflectionUtil.newInstance(groupClazz);
                if(groupInstance instanceof Command)
                    groupContainer.addChild(ParserGenerator.doGenerateCommandLineParser(
                            groupInstance, true, true));
                else
                    groupContainer.addChild(ParserGenerator.doGenerateCommandLineParser(
                            groupInstance, false, true));
            }

            return groupContainer;
        }
        else
            throw new CommandLineParserException("Commands must be annotated with @CommandDefinition or @GroupCommandDefinition");
    }

    private static void processCommand(ProcessedCommand processedCommand, Class<? extends Command> clazz) throws CommandLineParserException {
        for(Field field : clazz.getDeclaredFields()) {
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
                    throw new CommandLineParserException("OptionGroup field must be instance of Collection");
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
                        .create());
            }
        }

    }

    public static void parseAndPopulate(Object instance, String input) throws CommandLineParserException, OptionValidatorException {
        CommandLineParser cl = generateCommandLineParser(instance.getClass()).getParser();
        InvocationProviders invocationProviders = new AeshInvocationProviders(
                new AeshConverterInvocationProvider(),
                new AeshCompleterInvocationProvider(),
                new AeshValidatorInvocationProvider(),
                new AeshOptionActivatorProvider());
        cl.getCommandPopulator().populateObject(instance,  cl.parse(input), invocationProviders, null, true);
    }

}
