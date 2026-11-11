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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.aesh.AeshRuntimeRunner;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.junit.Test;

/**
 * What aesh itself writes through the invocation shell (command output and
 * the generated {@code --help}) reaches {@code commandOutputHandler} on the
 * direct {@code CommandRuntime} path — even without a shell on the builder
 * (#769).
 */
public class CommandOutputHandlerRuntimeTest {

    @Test
    public void testHandlerCapturesHelpWithoutShell() throws Exception {
        StringBuilder captured = new StringBuilder();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder
                .<CommandInvocation> builder()
                .commandRegistry(AeshCommandRegistryBuilder
                        .<CommandInvocation> builder().command(HelpCommand.class).create())
                .commandOutputHandler(captured::append)
                .build();

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("help --help"));
        assertTrue("Handler must capture the generated help, got: " + captured,
                captured.toString().contains("Usage:"));
    }

    @Test
    public void testHandlerCapturesCommandOutputWithoutShell() throws Exception {
        StringBuilder captured = new StringBuilder();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder
                .<CommandInvocation> builder()
                .commandRegistry(AeshCommandRegistryBuilder
                        .<CommandInvocation> builder().command(EchoCommand.class).create())
                .commandOutputHandler(captured::append)
                .build();

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("echo --msg hello"));
        assertTrue("Handler must capture command output, got: " + captured,
                captured.toString().contains("hello"));
    }

    @Test
    public void testSettingsHandlerCapturesHelpWithoutShell() throws Exception {
        // The settings-provided handler silently received nothing without a
        // shell before the null-shell tee fix.
        StringBuilder captured = new StringBuilder();
        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandOutputHandler(captured::append)
                .build();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder
                .<CommandInvocation> builder()
                .settings(settings)
                .commandRegistry(AeshCommandRegistryBuilder
                        .<CommandInvocation> builder().command(HelpCommand.class).create())
                .build();

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("help --help"));
        assertTrue("Handler must capture the generated help, got: " + captured,
                captured.toString().contains("Usage:"));
    }

    @Test
    public void testRunnerForwardsHandler() {
        StringBuilder captured = new StringBuilder();
        PrintStream original = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try {
            assertEquals(CommandResult.SUCCESS, AeshRuntimeRunner.builder()
                    .command(HelpCommand.class)
                    .commandOutputHandler(captured::append)
                    .args("--help")
                    .execute());
        } finally {
            System.setOut(original);
        }
        assertTrue("Runner must forward the handler, got: " + captured,
                captured.toString().contains("Usage:"));
    }

    @Test
    public void testRunnerForwardsHandlerToExecution() {
        // Non-lazy execution goes through the runtime builder, so the
        // handler must be forwarded there too.
        StringBuilder captured = new StringBuilder();
        PrintStream original = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try {
            assertEquals(CommandResult.SUCCESS, AeshRuntimeRunner.builder()
                    .command(EchoCommand.class)
                    .commandOutputHandler(captured::append)
                    .lazyStartup(false)
                    .args("--msg", "hello")
                    .execute());
        } finally {
            System.setOut(original);
        }
        assertTrue("Runner must forward the handler to execution, got: " + captured,
                captured.toString().contains("hello"));
    }

    @CommandDefinition(name = "help", description = "Help things", generateHelp = true)
    public static class HelpCommand implements Command<CommandInvocation> {
        @Option(description = "The color", defaultValue = "red")
        String color;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "echo", description = "Echo things")
    public static class EchoCommand implements Command<CommandInvocation> {
        @Option(name = "msg", required = true)
        private String msg;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println(msg);
            return CommandResult.SUCCESS;
        }
    }
}
