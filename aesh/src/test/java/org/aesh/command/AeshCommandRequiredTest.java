/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.AeshContext;
import org.aesh.console.ReadlineConsole;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * @author Aesh team
 */
public class AeshCommandRequiredTest {

    private final InvocationProviders invocationProviders = new AeshInvocationProviders();

    @Test
    public void testOptionRequired() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(ReqCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder
                .builder()
                .logging(true)
                .connection(connection)
                .commandRegistry(registry)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        connection.read("req " + Config.getLineSeparator());
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        connection.assertBufferEndsWith("Option: --reset-configuration is required for this command."
                + Config.getLineSeparator());

        console.stop();
    }

    @Test
    public void testArgumentAndOptionRequired() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(ReqCommand2.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder
                .builder()
                .logging(true)
                .connection(connection)
                .commandRegistry(registry)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        connection.read("req2 --reset-configuration=false" + Config.getLineSeparator());
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        connection.assertBufferEndsWith("Argument 'arg' is required for this command."
                + Config.getLineSeparator());

        console.stop();
    }

    @Test
    public void testArgumentAndOptionRequiredGroupCommand() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(GroupReqCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder
                .builder()
                .logging(true)
                .connection(connection)
                .commandRegistry(registry)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        connection.read("group req2 --reset-configuration=false" + Config.getLineSeparator());
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        connection.assertBufferEndsWith("Argument 'arg' is required for this command."
                + Config.getLineSeparator());

        console.stop();
    }

    @CommandDefinition(name = "req", description = "")
    public static class ReqCommand implements Command {

        @Option(name = "reset-configuration", required = true)
        private boolean resetConfiguration;

        @Option
        private String foo;

        @Argument
        private String arg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "group", description = "", groupCommands = { ReqCommand2.class })
    public static class GroupReqCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "req2", description = "")
    public static class ReqCommand2 implements Command {

        @Option(name = "reset-configuration", required = true)
        private boolean resetConfiguration;

        @Option
        private String foo;

        @Argument(required = true)
        private String arg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.println("resetConfiguration is: " + resetConfiguration + ", arg is:" + arg);
            return CommandResult.SUCCESS;
        }
    }

    // --- #698: a static defaultValue satisfies required ---

    @Test
    public void testRequiredOptionWithDefaultValueIsSatisfied() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new ReqDefaultCommand()).getParser();

        parser.populateObject("reqdef", invocationProviders, aeshContext, CommandLineParser.Mode.STRICT);
        assertTrue("Default must satisfy required: " + parser.getProcessedCommand().parserExceptions(),
                parser.getProcessedCommand().parserExceptions().isEmpty());
        assertEquals("lib", ((ReqDefaultCommand) parser.getCommand()).dir);

        parser.populateObject("reqdef --dir custom", invocationProviders, aeshContext,
                CommandLineParser.Mode.STRICT);
        assertTrue(parser.getProcessedCommand().parserExceptions().isEmpty());
        assertEquals("custom", ((ReqDefaultCommand) parser.getCommand()).dir);
    }

    @Test
    public void testRequiredArgumentWithDefaultValueIsSatisfied() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new ReqDefaultArgCommand()).getParser();

        parser.populateObject("reqdefarg", invocationProviders, aeshContext, CommandLineParser.Mode.STRICT);
        assertTrue("Default must satisfy required: " + parser.getProcessedCommand().parserExceptions(),
                parser.getProcessedCommand().parserExceptions().isEmpty());
        assertEquals("lib", ((ReqDefaultArgCommand) parser.getCommand()).dir);
    }

    @Test
    public void testRequiredOptionWithoutDefaultStillFails() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new ReqNoDefaultCommand()).getParser();

        parser.populateObject("reqnodef", invocationProviders, aeshContext, CommandLineParser.Mode.STRICT);
        assertEquals(1, parser.getProcessedCommand().parserExceptions().size());
        assertTrue(parser.getProcessedCommand().parserExceptions()
                .get(0) instanceof org.aesh.command.parser.RequiredOptionException);
    }

    @CommandDefinition(name = "reqdef", description = "")
    public static class ReqDefaultCommand implements Command {

        @Option(name = "dir", defaultValue = "lib", required = true)
        private String dir;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "reqdefarg", description = "")
    public static class ReqDefaultArgCommand implements Command {

        @Argument(required = true, defaultValue = "lib")
        private String dir;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "reqnodef", description = "")
    public static class ReqNoDefaultCommand implements Command {

        @Option(name = "dir", required = true)
        private String dir;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

}
