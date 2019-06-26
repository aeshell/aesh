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
package org.aesh;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.tty.TestConnection;
import org.junit.Test;

import static org.aesh.terminal.utils.Config.getLineSeparator;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshConsoleRunnerTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testSimple() throws InterruptedException {
        TestConnection connection = new TestConnection();

        AeshConsoleRunner runner = AeshConsoleRunner.builder()
                                           .connection(connection)
                                           .command(HelloCommand.class)
                                           .addExitCommand();
        runner.start();

        connection.read("hello"+ getLineSeparator());
        connection.assertBufferEndsWith("Hello from Aesh!"+getLineSeparator());

        runner.stop();
    }

    @Test(expected = RuntimeException.class)
    public void testNoCommand() {
        AeshConsoleRunner runner = AeshConsoleRunner.builder();
        runner.start();
    }

    @Test(expected = RuntimeException.class)
    public void testNoCommandInSettings() {
        TestConnection connection = new TestConnection(false);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder().create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator>
                settings = SettingsBuilder.builder()
                                   .logging(true)
                                   .connection(connection)
                                   .commandRegistry(registry)
                                   .build();

        AeshConsoleRunner runner = AeshConsoleRunner.builder().settings(settings);
        runner.start();
    }

    @Test
    public void testMultipleCommands() throws InterruptedException {
        TestConnection connection = new TestConnection();

        AeshConsoleRunner runner = AeshConsoleRunner.builder()
                                           .connection(connection)
                                           .commands(new Class[]{HelloCommand.class, Bar1Command.class, Bar2Command.class})
                                           .addExitCommand();

        runner.start();

        connection.read("bar1"+getLineSeparator());
        connection.assertBufferEndsWith("Hello from Bar1"+getLineSeparator());
        connection.read("exit"+getLineSeparator());
        Thread.sleep(200);
        assertTrue(connection.closed());

    }


    @CommandDefinition(name = "hello", description = "hello from aesh")
    public static class HelloCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            commandInvocation.println("Hello from Aesh!");
            return CommandResult.SUCCESS;
        }
    }

     @CommandDefinition(name = "bar1", description = "bar1")
    public static class Bar1Command implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            commandInvocation.println("Hello from Bar1");
            return CommandResult.SUCCESS;
        }
    }
     @CommandDefinition(name = "bar2", description = "bar2")
    public static class Bar2Command implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            commandInvocation.println("Hello from Bar2");
            return CommandResult.SUCCESS;
        }
    }



}
