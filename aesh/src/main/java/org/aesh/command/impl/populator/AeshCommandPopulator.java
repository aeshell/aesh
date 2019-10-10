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
package org.aesh.command.impl.populator;

import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.readline.AeshContext;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.Command;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.selector.SelectorType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandPopulator<O extends Object, CI extends CommandInvocation> implements CommandPopulator<O, CI> {

    private final O instance;

    public AeshCommandPopulator(O instance) {
        this.instance = instance;
    }

    /**
     * Populate a Command instance with the values parsed from a command line
     * If any parser errors are detected it will throw an exception
     * @param processedCommand command line
     * @param mode do validation or not
     * @throws CommandLineParserException any incorrectness in the parser will abort the populate
     */
    @Override
    public void populateObject(ProcessedCommand<Command<CI>, CI> processedCommand, InvocationProviders invocationProviders,
                               AeshContext aeshContext, CommandLineParser.Mode mode)
            throws CommandLineParserException, OptionValidatorException {
        if(processedCommand.parserExceptions().size() > 0 && mode == CommandLineParser.Mode.VALIDATE)
            throw processedCommand.parserExceptions().get(0);
        for(ProcessedOption option : processedCommand.getOptions()) {
            if(option.getValues() != null && option.getValues().size() > 0)
                option.injectValueIntoField(getObject(), invocationProviders, aeshContext,
                        mode == CommandLineParser.Mode.VALIDATE );
            else if(option.getDefaultValues().size() > 0 && option.selectorType() == SelectorType.NO_OP) {
                option.injectValueIntoField(getObject(), invocationProviders, aeshContext,
                        mode == CommandLineParser.Mode.VALIDATE);
            }
            else if(option.getOptionType().equals(OptionType.GROUP) && option.getProperties().size() > 0)
                option.injectValueIntoField(getObject(), invocationProviders, aeshContext,
                        mode == CommandLineParser.Mode.VALIDATE);
            else
                resetField(getObject(), option.getFieldName(), option.hasValue());
        }
        //arguments
        if(processedCommand.getArguments() != null &&
                (processedCommand.getArguments().getValues().size() > 0 ||
                         processedCommand.getArguments().getDefaultValues().size() > 0))
            processedCommand.getArguments().injectValueIntoField(getObject(), invocationProviders, aeshContext,
                    mode == CommandLineParser.Mode.VALIDATE);
        else if(processedCommand.getArguments() != null)
            resetField(getObject(), processedCommand.getArguments().getFieldName(), true);
        //argument
         if(processedCommand.getArgument() != null &&
                (processedCommand.getArgument().getValues().size() > 0 ||
                         processedCommand.getArgument().getDefaultValues().size() > 0))
            processedCommand.getArgument().injectValueIntoField(getObject(), invocationProviders, aeshContext,
                    mode == CommandLineParser.Mode.VALIDATE);
        else if(processedCommand.getArgument() != null)
            resetField(getObject(), processedCommand.getArgument().getFieldName(), true);
    }

    /**
     * Will parse the input line and populate the fields in the instance object specified by
     * the given annotations.
     * The instance object must be annotated with the CommandDefinition annotation @see CommandDefinition
     * Any parser errors will throw an exception
     *
     * @param instance command
     * @param fieldName field
    public static void parseAndPopulate(Object instance, String line)
            throws CommandLineParserException, OptionValidatorException {
        CommandLineParser parser = ParserGenerator.generateCommandLineParser(instance.getClass());
        AeshCommandPopulator populator = new AeshCommandPopulator(parser);
        populator.populateObject(instance, parser.parse(line));
    }
     */

    private void resetField(Object instance, String fieldName, boolean hasValue) {
        try {
            Field field = getField(instance.getClass(), fieldName);
            //for some options, the field might be null. eg generatedHelp
            //if so we ignore it
            if(field == null)
                return;
            if(!Modifier.isPublic(field.getModifiers()))
                field.setAccessible(true);
            if(field.getType().isPrimitive()) {
                if(boolean.class.isAssignableFrom(field.getType()))
                    field.set(instance, false);
                else if(int.class.isAssignableFrom(field.getType()))
                    field.set(instance, 0);
                else if(short.class.isAssignableFrom(field.getType()))
                    field.set(instance, 0);
                else if(char.class.isAssignableFrom(field.getType()))
                    field.set(instance, '\u0000');
                else if(byte.class.isAssignableFrom(field.getType()))
                    field.set(instance, 0);
                else if(long.class.isAssignableFrom(field.getType()))
                    field.set(instance, 0L);
                else if(float.class.isAssignableFrom(field.getType()))
                    field.set(instance, 0.0f);
                else if(double.class.isAssignableFrom(field.getType()))
                    field.set(instance, 0.0d);
            }
            else if(!hasValue && field.getType().equals(Boolean.class)) {
               field.set(instance, Boolean.FALSE);
            }
            else
                field.set(instance, null);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private Field getField(Class clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        }
        catch(NoSuchFieldException nsfe) {
            if(clazz.getSuperclass() != null)
                return getField(clazz.getSuperclass(), fieldName);
            else
                return null;
        }
    }

    @Override
    public O getObject() {
        return instance;
    }

}
