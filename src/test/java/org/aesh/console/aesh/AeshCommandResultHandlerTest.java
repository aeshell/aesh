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

import org.aesh.command.option.Arguments;
import org.aesh.command.CommandDefinition;
import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.console.settings.Settings;
import org.aesh.console.settings.SettingsBuilder;
import org.aesh.readline.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.aesh.utils.Config;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandResultHandlerTest {

    @Test
    public void testResultHandler() throws IOException, InterruptedException, CommandLineParserException {
        TestConnection connection = new TestConnection();

       CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(FooCommand.class)
                .create();

         Settings settings = SettingsBuilder.builder()
                 .commandRegistry(registry)
                 .connection(connection)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("foo --foo 1 --name aesh"+ Config.getLineSeparator());
        //outputStream.flush();
        Thread.sleep(80);

        connection.read("foo --foo 1"+ Config.getLineSeparator());
        //outputStream.flush();
        Thread.sleep(80);

        connection.read("foo --fo 1 --name aesh"+ Config.getLineSeparator());
        //outputStream.flush();
        Thread.sleep(80);

        connection.read("foo --foo 1 --exception" + Config.getLineSeparator());
        //outputStream.flush();
        Thread.sleep(80);

    }


    @CommandDefinition(name = "foo", description = "", resultHandler = FooResultHandler.class)
    public static class FooCommand implements Command {

        @Option(required = true)
        private String foo;

        @Option
        private String name;

        @Option(hasValue = false)
        private boolean exception;

        @Arguments
        private List<String> arguments;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            if (name == null) {
                if (exception) {
                    throw new CommandException("Exception occured, please fix options");
                } else {
                    return CommandResult.FAILURE;
                }
            } else
                return CommandResult.SUCCESS;
        }

        public String getName() {
            return name;
        }
    }

    public static class FooResultHandler implements ResultHandler {

        private transient int resultCounter = 0;

        @Override
        public void onSuccess() {
            assertEquals(0, resultCounter);
            resultCounter++;
        }

        @Override
        public void onFailure(CommandResult result) {
            assertEquals(1, resultCounter);
            resultCounter++;
        }

        @Override
        public void onValidationFailure(CommandResult result, Exception exception) {
            assertEquals(2, resultCounter);
            resultCounter++;
        }

        @Override
        public void onExecutionFailure(CommandResult result, CommandException exception) {
            assertEquals(3, resultCounter);
        }
    }

}
