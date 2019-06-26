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
package org.aesh.command.builder;

import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.CommandLineParserBuilder;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.readline.AeshContext;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainer;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.readline.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.aesh.terminal.utils.Config;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.aesh.command.CommandException;

import static org.junit.Assert.assertEquals;

/**
 *
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandCustomCommand {

    @Test
    public void testCustom() throws Exception {

        TestConnection connection = new TestConnection();

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                                                              .command(createBuilder())
                                                              .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .commandRegistry(registry)
                        .connection(connection)
                        .logging(true)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);

        console.start();

        connection.read("foo --bar YES");
        connection.read(Config.getLineSeparator());
        Thread.sleep(80);

        console.stop();
    }

    private CommandContainer<CommandInvocation> createBuilder() throws CommandLineParserException {

        CustomPopulator populator = new CustomPopulator();

        ProcessedCommand<CustomCommand, CommandInvocation> processedCommand =
                ProcessedCommandBuilder.<CustomCommand,CommandInvocation>builder()
                        .name("foo")
                        .description("this is foo")
                        .populator(populator)
                        .command(new CustomCommand(populator))
                        .addOption(ProcessedOptionBuilder.builder()
                                        .name("bar")
                                        .type(String.class)
                                        .build()
                        )
                        .create();

        CommandLineParser<CommandInvocation> parser = CommandLineParserBuilder.<CustomCommand, CommandInvocation>builder()
                .processedCommand(processedCommand)
                .create();

        return new AeshCommandContainer<>(parser);
    }
}


class CustomCommand implements Command<CommandInvocation> {

    private CustomPopulator populator;

    CustomCommand(CustomPopulator populator) {
        this.populator = populator;
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        assertEquals(populator.getValue("bar"), "YES");
        return CommandResult.SUCCESS;
    }
}

class CustomPopulator implements CommandPopulator<Object, CommandInvocation> {

    private Map<String, String> values;
    private List<String> arguments;

    CustomPopulator() {
        values = new HashMap<>();
        arguments = new ArrayList<>();
    }

    public void addName(String name) {
        values.put(name, null);
    }

    public String getValue(String fieldName) {
        return values.get(fieldName);
    }

    public void putValue(String fieldName, String value) {
        values.put(fieldName, value);
    }

    public void clearValues() {
        for(String key : values.keySet())
            values.put(key, null);
        arguments.clear();
    }

    public void clearValue(String name) {
        values.put(name, null);
    }

    public void putArgumentValue(String value) {
        arguments.add(value);
    }

    public void clearArguments() {
        arguments.clear();
    }

    @Override
    public void populateObject(ProcessedCommand<Command<CommandInvocation>, CommandInvocation> processedCommand, InvocationProviders invocationProviders, AeshContext aeshContext, CommandLineParser.Mode validate) throws CommandLineParserException {
        if(processedCommand.parserExceptions().size() > 0)
            throw processedCommand.parserExceptions().get(0);
        for(ProcessedOption option : processedCommand.getOptions()) {
            if(option.getValue() != null)
                putValue(option.name(), option.getValue());
                //line.getOption(option.name()).injectValueIntoField(getObject(), invocationProviders, aeshContext, validate);
            /*
            else if(option.getDefaultValues().size() > 0) {
                putValue(option.name(), (String) processedCommand.getOption(option.name()).getDefaultValues().get(0));
                //option.injectValueIntoField(getObject(), invocationProviders, aeshContext, validate);
            }
            */
            else
                clearValue(option.getFieldName());
        }
        if((processedCommand.getArguments() != null && processedCommand.getArguments().getValues().size() > 0)) {
            //line.getArguments().injectValueIntoField(getObject(), invocationProviders, aeshContext, validate);
            putArgumentValue(processedCommand.getArguments().getValue());
        }
        else if(processedCommand.getArguments() != null)
            clearArguments();
     }

    @Override
    public Object getObject() {
        return values;
    }
}
