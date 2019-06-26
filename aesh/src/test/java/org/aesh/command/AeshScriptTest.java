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
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.shell.Shell;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.readline.ReadlineConsole;
import org.aesh.terminal.utils.Config;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.tty.TestConnection;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 *
 * the idea of this test is to show how aesh could work reading a script file.
 * this impl will only accept "foo" commands,
 * if any other commands are found it should jump out and exit.
 *
 * the scriptfile:
 * foo //this is accepted
 * foo // also accepted
 *
 * exit //should exit
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshScriptTest {

    private static CountDownLatch counter = new CountDownLatch(3);

    @Test
    public void scriptPoc() throws IOException, CommandLineParserException, InterruptedException, CommandRegistryException {

        TestConnection connection = new TestConnection();

        CommandResultHandler resultHandler = new CommandResultHandler();
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> fooCommand = ProcessedCommandBuilder.builder()
                                              .name("foo")
                                              .resultHandler(resultHandler)
                                              .command(FooCommand.class)
                                              .create();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                                           .command(fooCommand)
                                           .command(BarCommand.class)
                                           .command(new RunCommand(resultHandler))
                                           .command(ExitCommand.class)
                                           .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .logging(true)
                        .commandRegistry(registry)
                        .connection(connection)
                        .commandNotFoundHandler(resultHandler)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);

        console.start();

        connection.read("run");
        connection.read(Config.getLineSeparator());
        Thread.sleep(20);

        counter.await(1, TimeUnit.SECONDS);

        assertEquals(0, counter.getCount());
        console.stop();
    }

    private List<String> readScriptFile() throws IOException {
        List<String> lines = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader("src/test/resources/script1"));
        String line = br.readLine();
        while (line != null) {
            if (line.trim().length() > 0 && !line.trim().startsWith("#"))
                lines.add(line);
            line = br.readLine();
        }

        return lines;
    }

    @CommandDefinition(name = "run", description = "")
    private class RunCommand implements Command {

        private final CommandResultHandler resultHandler;

        RunCommand(CommandResultHandler resultHandler) {
            this.resultHandler = resultHandler;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            try {

                List<String> script = readScriptFile();

                for (String line : script) {
                    commandInvocation.executeCommand(line);
                }

                return CommandResult.SUCCESS;
            } catch (Exception ex) {
                throw new CommandException(ex);
            }
        }
    }

    @CommandDefinition(name ="exit", description = "")
    private static class ExitCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.println("EXITING");
            commandInvocation.stop();
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "foo", description = "")
    private static class FooCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            if(counter.getCount() > 1) {
                commandInvocation.println("computing...." + Config.getLineSeparator() + "finished computing, returning...");
                counter.countDown();
                return CommandResult.SUCCESS;
            }
            else {
                fail();
                return CommandResult.FAILURE;
            }
        }
    }

    @CommandDefinition(name = "bar", description = "")
    private static class BarCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            if(counter.getCount() == 1) {
                commandInvocation.println("baring...." + Config.getLineSeparator() + "finished baring, returning...");
                counter.countDown();
                return CommandResult.SUCCESS;
            }
            else {
                fail();
                return CommandResult.FAILURE;
            }
        }
    }

    private static class CommandResultHandler implements ResultHandler, CommandNotFoundHandler {

        private boolean failed = false;
        private String failedString;

        @Override
        public void onSuccess() {

        }

        @Override
        public void onFailure(CommandResult result) {
            failed = true;
        }

        @Override
        public void onValidationFailure(CommandResult result, Exception exception) {
            failed = true;
            failedString = exception.toString();
        }

        public void setFailed(boolean f) {
            failed = f;
        }

        public boolean hasFailed() {
            return failed;
        }

        public String getFailedReason() {
            return failedString;
        }

        @Override
        public void handleCommandNotFound(String line, Shell shell) {
            failed = true;
            failedString = line;
        }

        @Override
        public void onExecutionFailure(CommandResult result, CommandException exception) {
            failed = true;
            failedString = exception.toString();
        }
    }

}
