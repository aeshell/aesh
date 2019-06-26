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
package org.aesh.command;

import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.readline.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.aesh.terminal.utils.Config;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandMultipleInvocations {

    private static int counter = 0;

    @Test
    public void testMultipleInvocations() throws CommandRegistryException, IOException, InterruptedException {
        TestConnection connection = new TestConnection();

         CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                 .command(FooCommand.class)
                 .command(BarCommand.class)
                 .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                                OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .commandRegistry(registry)
                        .enableOperatorParser(true)
                        .connection(connection)
                        .setPersistExport(false)
                        .logging(true)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("foo ;bar --info yup; foo --value=VAL" + Config.getLineSeparator());
        Thread.sleep(100);
        assertEquals(2, counter);

        console.stop();
    }

    @Test
    public void testMultipleInvocationsClearLine() throws CommandRegistryException, IOException, InterruptedException {
        TestConnection connection = new TestConnection();

         CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                 .command(FooCommand.class)
                 .command(BarCommand.class)
                 .command(BarBarCommand.class)
                 .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                                OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .commandRegistry(registry)
                        .enableOperatorParser(true)
                        .connection(connection)
                        .setPersistExport(false)
                        .logging(true)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();
        counter = 0;

        connection.read("foo ;barbar && foo --value=VAL" + Config.getLineSeparator());
        Thread.sleep(100);
        assertEquals(1, counter);
        connection.clearOutputBuffer();
        counter = 0;
        connection.read("foo" + Config.getLineSeparator());

        console.stop();
    }

    @Test
    public void testMultipleInvocationsRequiredOption() throws CommandRegistryException, IOException, InterruptedException {
        TestConnection connection = new TestConnection();

         CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                 .command(FooCommand.class)
                 .command(BarCommand.class)
                 .command(RequiredBarCommand.class)
                 .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                                OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .commandRegistry(registry)
                        .enableOperatorParser(true)
                        .connection(connection)
                        .setPersistExport(false)
                        .logging(true)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();
        counter = 0;

        connection.read("foo ;req && foo --value=VAL" + Config.getLineSeparator());
        Thread.sleep(100);
        assertEquals(1, counter);
        connection.clearOutputBuffer();
        connection.read("foo" + Config.getLineSeparator());
        Thread.sleep(20);
        assertEquals(2, counter);

        console.stop();
    }



    @CommandDefinition(name ="foo", description = "")
    private static class FooCommand implements Command {

        @Option
        private String value;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            if(counter == 0) {
                assertNull(value);
                counter++;
            }
            else if(counter == 1) {
                assertNull(value);
                counter++;
            }

            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name ="bar", description = "")
    private static class BarCommand implements Command {

        @Option
        private String info;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            assertEquals("yup", info);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name ="barbar", description = "")
    private static class BarBarCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            throw new CommandException("We're doing and exception");
        }
    }

    @CommandDefinition(name ="req", description = "")
    private static class RequiredBarCommand implements Command {

        @Option(required = true)
        private String required;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

}
