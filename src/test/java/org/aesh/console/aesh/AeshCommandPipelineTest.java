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

import org.aesh.command.CommandException;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.console.settings.Settings;
import org.aesh.console.settings.SettingsBuilder;
import org.aesh.command.CommandDefinition;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.Command;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.CommandResult;
import org.aesh.readline.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.aesh.utils.Config;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandPipelineTest {

    @Test
    public void testPipeline() throws InterruptedException, IOException {
        TestConnection connection = new TestConnection();

        FooCommand foo = new FooCommand();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(PipeCommand.class)
                .command(foo)
                .create();

        Settings settings = SettingsBuilder.builder()
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
        console.stop();
    }

    @CommandDefinition(name ="pipe", description = "")
    public static class PipeCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.println("hello");
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
            } catch (IOException ex) {
                throw new CommandException(ex);
            }
            return CommandResult.SUCCESS;
        }

        public int getCounter() {
            return counter;
        }
    }
}
