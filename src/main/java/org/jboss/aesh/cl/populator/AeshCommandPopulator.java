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
package org.jboss.aesh.cl.populator;

import org.jboss.aesh.cl.CommandLine;
import org.jboss.aesh.cl.internal.ProcessedInputPrompt;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshConsoleBufferBuilder;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.AeshInputProcessorBuilder;
import org.jboss.aesh.console.ConsoleBuffer;
import org.jboss.aesh.console.InputProcessor;
import org.jboss.aesh.console.InvocationProviders;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.invocation.CommandInvocation;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandPopulator implements CommandPopulator<Object, Command> {

    private final Object instance;

    public AeshCommandPopulator(Object instance) {
        this.instance = instance;
    }

    /**
     * Populate a Command instance with the values parsed from a command line
     * If any parser errors are detected it will throw an exception
     * @param line command line
     * @param validate do validation or not
     * @throws CommandLineParserException
     */
    @Override
    public void populateObject(CommandLine<Command> line, InvocationProviders invocationProviders,
                               AeshContext aeshContext, boolean validate, CommandInvocation commandInvocation)
            throws CommandLineParserException, OptionValidatorException, InterruptedException, IOException {
        if(line.hasParserError())
            throw line.getParserException();
        for(ProcessedOption option : line.getParser().getProcessedCommand().getOptions()) {
            if(line.hasOption(option.getName()))
                line.getOption(option.getName()).injectValueIntoField(getObject(), invocationProviders, aeshContext, validate);
            else if(option.getDefaultValues().size() > 0) {
                option.injectValueIntoField(getObject(), invocationProviders, aeshContext, validate);
            }
            else
                resetField(getObject(), option.getFieldName(), option.hasValue());
        }
        if((line.getArgument() != null && line.getArgument().getValues().size() > 0) ||
                (line.getParser().getProcessedCommand().getArgument() != null &&
                        line.getParser().getProcessedCommand().getArgument().getDefaultValues().size() > 0)) {
            line.getArgument().injectValueIntoField(getObject(), invocationProviders, aeshContext, validate);
        }
        else if(line.getArgument() != null)
            resetField(getObject(), line.getArgument().getFieldName(), true);

		if (commandInvocation != null) {
			// sort by order
			Collections.sort(line.getParser().getProcessedCommand().getInputPrompts(),
					new Comparator<ProcessedInputPrompt>() {
						@Override
						public int compare(ProcessedInputPrompt o1, ProcessedInputPrompt o2) {
							return Integer.compare(o1.getOrder(), o2.getOrder());
						}
					});
			// prompt for input and set value on command
			for (ProcessedInputPrompt inputPrompt : line.getParser().getProcessedCommand().getInputPrompts()) {
				ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder()
						.shell(commandInvocation.getShell())
						.prompt(new Prompt(inputPrompt.getPrompt(), inputPrompt.getMask()))
						.create();
				InputProcessor inputProcessor = new AeshInputProcessorBuilder()
						.consoleBuffer(consoleBuffer)
						.create();
				consoleBuffer.displayPrompt();
				String result;
				do {
					result = inputProcessor.parseOperation(commandInvocation.getInput());
				}
				while(result == null );

				inputPrompt.injectValueIntoField(instance, result);
			}
		}
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
            else throw nsfe;
        }
    }

    @Override
    public Object getObject() {
        return instance;
    }

}
