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
package org.jboss.aesh.console.aesh;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.readline.ReadlineConsole;
import org.jboss.aesh.tty.TestConnection;
import org.aesh.util.Config;
import org.junit.Test;

import java.io.IOException;
import org.jboss.aesh.console.command.CommandException;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandEndOperatorTest {

    private static int counter = 0;

    @Test
    public void testEnd() throws IOException, InterruptedException {
        TestConnection connection = new TestConnection();

         CommandRegistry registry = new AeshCommandRegistryBuilder()
                 .command(FooCommand.class)
                 .command(BarCommand.class)
                 .create();

        Settings settings = new SettingsBuilder()
                .commandRegistry(registry)
                .connection(connection)
                .setPersistExport(false)
                .logging(true)
                .create();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("foo ;bar --info yup; foo"+ Config.getLineSeparator());
        //outputStream.flush();
        Thread.sleep(100);
        assertEquals(3, counter);

        console.stop();
    }

    @CommandDefinition(name ="foo", description = "")
    private static class FooCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            counter++;
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
            counter++;
            return CommandResult.SUCCESS;
        }
    }

}
