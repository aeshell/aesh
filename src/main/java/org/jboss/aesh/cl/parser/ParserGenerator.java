/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.parser;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.OptionGroup;
import org.jboss.aesh.cl.OptionList;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.internal.OptionType;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshInvocationProviders;
import org.jboss.aesh.console.InvocationProviders;
import org.jboss.aesh.console.command.completer.AeshCompleterInvocationProvider;
import org.jboss.aesh.console.command.converter.AeshConverterInvocationProvider;
import org.jboss.aesh.console.command.validator.AeshValidatorInvocationProvider;

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

    public static CommandLineParser generateCommandLineParser(Object paramInstance) throws CommandLineParserException {
        return generateCommandLineParser(paramInstance.getClass());
    }

    public static CommandLineParser generateCommandLineParser(Class clazz) throws CommandLineParserException {
        CommandDefinition command = (CommandDefinition) clazz.getAnnotation(CommandDefinition.class);
        if(command == null)
            throw new CommandLineParserException("Commands must be annotated with @CommandDefinition");

        ProcessedCommand processedCommand = new ProcessedCommand(command.name(), command.description(),
                command.validator(), command.resultHandler());

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
                    processedCommand.addOption(o.shortName(), field.getName(), o.description(),
                            o.argument(), o.required(), ',', o.defaultValue(),
                            field.getType(), field.getName(), optionType, o.converter(),
                            o.completer(), o.validator(), o.activator(), o.renderer(), o.overrideRequired());
                }
                else {
                    processedCommand.addOption(o.shortName(), o.name(), o.description(),
                            o.argument(), o.required(), ',', o.defaultValue(),
                            field.getType(), field.getName(), optionType, o.converter(),
                            o.completer(), o.validator(), o.activator(), o.renderer(), o.overrideRequired());
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
                    processedCommand.addOption(ol.shortName(), field.getName(), ol.description(), "",
                            ol.required(), ol.valueSeparator(), ol.defaultValue(), type, field.getName(), OptionType.LIST,
                            ol.converter(), ol.completer(), ol.validator(), ol.activator(), ol.renderer());
                }
                else {
                    processedCommand.addOption(ol.shortName(), ol.name(), ol.description(), "",
                            ol.required(), ol.valueSeparator(), ol.defaultValue(), type, field.getName(), OptionType.LIST,
                            ol.converter(), ol.completer(), ol.validator(), ol.activator(), ol.renderer());
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
                if(og.name() == null || og.name().length() < 1) {
                    processedCommand.addOption(og.shortName(), field.getName(), og.description(),
                            "", og.required(), ',', og.defaultValue(), type, field.getName(), OptionType.GROUP,
                            og.converter(), og.completer(), og.validator(), og.activator(), og.renderer());
                }
                else {
                    processedCommand.addOption(og.shortName(), og.name(), og.description(),
                            "", og.required(), ',', og.defaultValue(), type, field.getName(), OptionType.GROUP,
                            og.converter(), og.completer(), og.validator(), og.activator(), og.renderer());
                }
            }

            else if((a = field.getAnnotation(Arguments.class)) != null) {
                if(!Collection.class.isAssignableFrom(field.getType()))
                    throw new CommandLineParserException("Arguments field must be instance of Collection");
                Class type = Object.class;
                if(field.getGenericType() != null) {
                    ParameterizedType listType = (ParameterizedType) field.getGenericType();
                    type = (Class) listType.getActualTypeArguments()[0];
                }
                processedCommand.setArgument(
                        new ProcessedOption('\u0000',"", a.description(), "", false, a.valueSeparator(),
                                a.defaultValue(), type, field.getName(), OptionType.ARGUMENT, a.converter(),
                                a.completer(), a.validator(), null, null));
            }
        }

        return new CommandLineParserBuilder().parameter(processedCommand).generateParser();
    }

    public static void parseAndPopulate(Object instance, String input) throws CommandLineParserException, OptionValidatorException {
        CommandLineParser cl = generateCommandLineParser(instance.getClass());
        InvocationProviders invocationProviders = new AeshInvocationProviders(
                new AeshConverterInvocationProvider(),
                new AeshCompleterInvocationProvider(),
                new AeshValidatorInvocationProvider());
        cl.getCommandPopulator().populateObject(instance,  cl.parse(input), invocationProviders, null, true);
    }

}
