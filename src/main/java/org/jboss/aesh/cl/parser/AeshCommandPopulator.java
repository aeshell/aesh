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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aesh.cl.parser;

import org.jboss.aesh.cl.CommandLine;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.InvocationProviders;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandPopulator implements CommandPopulator<Object> {

    private final CommandLineParser commandLineParser;

    public AeshCommandPopulator(CommandLineParser commandLineParser) {
        this.commandLineParser = commandLineParser;
    }

    /**
     * Populate a Command instance with the values parsed from a command line
     * If any parser errors are detected it will throw an exception
     * @param instance command
     * @param line command line
     * @param validate do validation or not
     * @throws CommandLineParserException
     */
    @Override
    public void populateObject(Object instance, CommandLine line, InvocationProviders invocationProviders,
                               AeshContext aeshContext, boolean validate)
            throws CommandLineParserException, OptionValidatorException {
        if(line.hasParserError())
            throw line.getParserException();
        for(ProcessedOption option: commandLineParser.getProcessedCommand().getOptions()) {
            if(line.hasOption(option.getName()))
                line.getOption(option.getName()).injectValueIntoField(instance, invocationProviders, aeshContext, validate);
            else if(option.getDefaultValues().size() > 0) {
                option.injectValueIntoField(instance, invocationProviders, aeshContext, validate);
            }
            else
                resetField(instance, option.getFieldName(), option.hasValue());
        }
        if((line.getArgument() != null && line.getArgument().getValues().size() > 0) ||
                (commandLineParser.getProcessedCommand().getArgument() != null &&
                        commandLineParser.getProcessedCommand().getArgument().getDefaultValues().size() > 0)) {
            line.getArgument().injectValueIntoField(instance, invocationProviders, aeshContext, validate);
        }
        else if(line.getArgument() != null)
            resetField(instance, line.getArgument().getFieldName(), true);
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
            Field field = instance.getClass().getDeclaredField(fieldName);
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

}
