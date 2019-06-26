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
package org.aesh.command.operator;

import org.aesh.command.CommandException;
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.CommandDefinition;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.Command;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.CommandResult;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.io.Resource;
import org.aesh.readline.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.aesh.terminal.utils.Config;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandPipelineTest {

    @Test
    public void testPipeline() throws InterruptedException, IOException, CommandRegistryException {
        TestConnection connection = new TestConnection();

        FooCommand foo = new FooCommand();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(PipeCommand.class)
                .command(BarCommand.class)
                .command(foo)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation,
                        ValidatorInvocation, OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .connection(connection)
                        .enableOperatorParser(true)
                        .commandRegistry(registry)
                        .logging(true)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("pipe | foo" + Config.getLineSeparator());
        Thread.sleep(100);
        assertEquals(1, foo.getCounter());

        connection.read("pipe | bar" + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("hello aesh"+Config.getLineSeparator());
        console.stop();
    }


    @Test
    public void testEmptyBeforeOperator() throws InterruptedException, IOException, CommandRegistryException {
        TestConnection connection = new TestConnection();

        FooCommand foo = new FooCommand();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(PipeCommand.class)
                .command(BarCommand.class)
                .command(foo)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .connection(connection)
                        .enableOperatorParser(true)
                        .commandRegistry(registry)
                        .logging(true)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("&&" + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("aesh: syntax error near unexpected token \'&&\'"+Config.getLineSeparator());
        console.stop();
    }

    @CommandDefinition(name ="pipe", description = "")
    public static class PipeCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.println("hello");
            commandInvocation.println("aesh");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "foo", description = "")
    public static class FooCommand implements Command {
        private int counter = 0;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            try {
                if (commandInvocation.getConfiguration().getPipedData().available() > 0) {
                    counter++;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(commandInvocation.getConfiguration().getPipedData()));

                assertEquals("hello", reader.readLine());
                assertEquals("aesh", reader.readLine());
            } catch (IOException ex) {
                throw new CommandException(ex);
            }
            return CommandResult.SUCCESS;
        }

        int getCounter() {
            return counter;
        }
    }

    @CommandDefinition(name = "bar", description = "")
    public static class BarCommand implements Command {

        @Argument
        Resource arg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(arg.read()));
                commandInvocation.println(reader.readLine() + " " + reader.readLine());
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            return CommandResult.SUCCESS;
        }

    }

}
