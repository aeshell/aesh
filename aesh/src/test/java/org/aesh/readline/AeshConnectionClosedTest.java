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
package org.aesh.readline;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.readline.terminal.Key;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

import java.util.function.Consumer;

import static org.junit.Assert.assertTrue;

public class AeshConnectionClosedTest {

    @Test
    public void testConnectionClosed() throws InterruptedException {
        TestConnection connection = new TestConnection();

        ConnectionClosed closed = new ConnectionClosed();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                OptionActivator, CommandActivator > settings =
                SettingsBuilder.builder()
                        .connection(connection)
                        .setConnectionClosedHandler(closed)
                        .logging(true)
                        .build();

        AeshConsoleRunner console = AeshConsoleRunner.builder()
                .settings(settings)
                .command(TestCommand.class)
                .connection(connection).addExitCommand();

        console.start();

        connection.read("test"+ Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("hello"+Config.getLineSeparator());
        connection.read(Key.CTRL_D);
        Thread.sleep(100);
        assertTrue(closed.notifiedOfClose);
    }


    public static class ConnectionClosed implements Consumer<Void> {

        public transient boolean notifiedOfClose = false;

        @Override
        public void accept(Void aVoid) {
            notifiedOfClose = !notifiedOfClose;
        }
    }

    @CommandDefinition(name = "test", description = "")
    public static class TestCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.println("hello");
            return CommandResult.SUCCESS;
        }
    }
}
