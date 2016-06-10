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

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.result.ResultHandler;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.List;
import org.jboss.aesh.console.command.CommandException;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandResultHandlerTest {

    @Test
    public void testResultHandler() throws IOException, InterruptedException {
               PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .logging(true)
                .create();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(FooCommand.class)
                .create();

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry)
                .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();
        aeshConsole.start();

        outputStream.write(("foo --foo 1 --name aesh"+ Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(80);

        outputStream.write(("foo --foo 1"+ Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(80);

        outputStream.write(("foo --fo 1 --name aesh"+ Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(80);

        outputStream.write(("foo --foo 1 --exception" + Config.getLineSeparator()).getBytes());
        outputStream.flush();
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
