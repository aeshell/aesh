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
package org.aesh.console.aesh;

import org.aesh.cl.internal.ProcessedCommandBuilder;
import org.aesh.cl.internal.ProcessedOption;
import org.aesh.cl.internal.ProcessedOptionBuilder;
import org.aesh.cl.parser.CommandLineParser;
import org.aesh.cl.populator.CommandPopulator;
import org.aesh.console.command.invocation.CommandInvocation;
import org.aesh.console.settings.Settings;
import org.aesh.console.settings.SettingsBuilder;
import org.aesh.cl.CommandLine;
import org.aesh.cl.internal.ProcessedCommand;
import org.aesh.cl.parser.CommandLineParserBuilder;
import org.aesh.cl.parser.CommandLineParserException;
import org.aesh.console.AeshContext;
import org.aesh.console.InvocationProviders;
import org.aesh.console.command.Command;
import org.aesh.console.command.CommandResult;
import org.aesh.console.command.container.AeshCommandContainer;
import org.aesh.console.command.container.CommandContainer;
import org.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.aesh.console.command.registry.CommandRegistry;
import org.aesh.readline.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.aesh.util.Config;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.aesh.console.command.CommandException;

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

       CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(createBuilder())
                .create();

        Settings settings = new SettingsBuilder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .create();

         ReadlineConsole console = new ReadlineConsole(settings);

        console.start();

        connection.read("foo --bar YES");
        connection.read(Config.getLineSeparator());
        Thread.sleep(80);

        console.stop();
    }

    private CommandContainer createBuilder() throws CommandLineParserException {

        CustomPopulator populator = new CustomPopulator();

        ProcessedCommand processedCommand =
                new ProcessedCommandBuilder()
                        .name("foo")
                        .description("this is foo")
                        .populator(populator)
                        .command(new CustomCommand(populator))
                        .addOption(new ProcessedOptionBuilder()
                                        .name("bar")
                                        .type(String.class)
                                        .create()
                        )
                        .create();

        CommandLineParser parser = new CommandLineParserBuilder()
                .processedCommand(processedCommand)
                .create();

        return new AeshCommandContainer(processedCommand);
    }
}


class CustomCommand implements Command {

    private CustomPopulator populator;

    public CustomCommand(CustomPopulator populator) {
        this.populator = populator;
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        assertEquals(populator.getValue("bar"), "YES");
        return CommandResult.SUCCESS;
    }
}

class CustomPopulator implements CommandPopulator<Object, Command> {

    private Map<String, String> values;
    private List<String> arguments;

    public CustomPopulator() {
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
    public void populateObject(CommandLine<Command> line, InvocationProviders invocationProviders, AeshContext aeshContext, boolean validate) throws CommandLineParserException {
        if(line.hasParserError())
            throw line.getParserException();
        for(ProcessedOption option : line.getParser().getProcessedCommand().getOptions()) {
            if(line.hasOption(option.getName()))
                putValue(option.getName(), (String) line.getOption(option.getName()).getValue());
                //line.getOption(option.getName()).injectValueIntoField(getObject(), invocationProviders, aeshContext, validate);
            else if(option.getDefaultValues().size() > 0) {
                putValue(option.getName(), (String) line.getOption(option.getName()).getDefaultValues().get(0));
                //option.injectValueIntoField(getObject(), invocationProviders, aeshContext, validate);
            }
            else
                clearValue(option.getFieldName());
        }
        if((line.getArgument() != null && line.getArgument().getValues().size() > 0) ||
                (line.getParser().getProcessedCommand().getArgument() != null &&
                        line.getParser().getProcessedCommand().getArgument().getDefaultValues().size() > 0)) {
            //line.getArgument().injectValueIntoField(getObject(), invocationProviders, aeshContext, validate);
            putArgumentValue((String) line.getArgument().getValue());
        }
        else if(line.getArgument() != null)
            clearArguments();
     }

    @Override
    public Object getObject() {
        return values;
    }
}
