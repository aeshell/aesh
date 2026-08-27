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
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.ReadlineConsole;
import org.aesh.terminal.Key;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class SubCommandModeConsoleTest {

    @Test
    public void testSubCommandModeHelpOnChild() throws Exception {
        TestConnection connection = new TestConnection();
        AtomicReference<CountDownLatch> latchRef = new AtomicReference<>(new CountDownLatch(1));

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(AppGroupCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .commandExecutionListener((line, result, durationMs) -> latchRef.get().countDown())
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // Type "app" + enter to enter sub-command mode
        connection.read("app" + Config.getLineSeparator());
        assertTrue("app should complete", latchRef.get().await(5, TimeUnit.SECONDS));

        String output = connection.getOutputBuffer();
        assertTrue("should show entering message", output.contains("Entering app mode"));
        connection.clearOutputBuffer();

        // Now in sub-command mode — type "build --help" + enter
        latchRef.set(new CountDownLatch(1));
        connection.read("build --help" + Config.getLineSeparator());
        assertTrue("build --help should complete", latchRef.get().await(5, TimeUnit.SECONDS));

        output = connection.getOutputBuffer();
        assertTrue("help should contain command name 'build'", output.contains("build"));
        assertTrue("help should contain --target option", output.contains("target"));
        assertTrue("help should contain --clean option", output.contains("clean"));
        connection.clearOutputBuffer();

        // Type "deploy --help" + enter
        latchRef.set(new CountDownLatch(1));
        connection.read("deploy --help" + Config.getLineSeparator());
        assertTrue("deploy --help should complete", latchRef.get().await(5, TimeUnit.SECONDS));

        output = connection.getOutputBuffer();
        assertTrue("deploy help should contain 'deploy'", output.contains("deploy"));
        assertTrue("deploy help should contain --environment", output.contains("environment"));
        connection.clearOutputBuffer();

        // Exit sub-command mode
        connection.read("exit" + Config.getLineSeparator());

        console.stop();
    }

    @Test
    public void testSubCommandModeCompletion() throws Exception {
        TestConnection connection = new TestConnection();
        AtomicReference<CountDownLatch> latchRef = new AtomicReference<>(new CountDownLatch(1));

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(AppGroupCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .commandExecutionListener((line, result, durationMs) -> latchRef.get().countDown())
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // Enter sub-command mode
        connection.read("app" + Config.getLineSeparator());
        assertTrue("app should complete", latchRef.get().await(5, TimeUnit.SECONDS));
        connection.clearOutputBuffer();

        // Tab-complete "bu" should complete to "build "
        connection.read("bu");
        connection.read(Key.CTRL_I);
        connection.waitForOutputContaining("build ", 5000);
        connection.assertBuffer("build ");

        // Ctrl+C exits sub-command mode (exitOnCtrlC=true by default)
        connection.read(Key.CTRL_C);
        // Re-enter sub-command mode
        latchRef.set(new CountDownLatch(1));
        connection.read("app" + Config.getLineSeparator());
        assertTrue("re-enter app should complete", latchRef.get().await(5, TimeUnit.SECONDS));
        connection.clearOutputBuffer();

        connection.read("de");
        connection.read(Key.CTRL_I);
        connection.waitForOutputContaining("deploy ", 5000);
        connection.assertBuffer("deploy ");

        connection.read("exit" + Config.getLineSeparator());
        console.stop();
    }

    @Test
    public void testSubCommandModeOptionNameCompletion() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(AppGroupCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // Enter sub-command mode
        connection.read("app" + Config.getLineSeparator());
        assertTrue("app should complete", latch.await(5, TimeUnit.SECONDS));
        connection.clearOutputBuffer();

        // Type "build --targ" + tab — should complete to "build --target "
        connection.read("build --targ");
        connection.read(Key.CTRL_I);
        connection.waitForOutputContaining("build --target ", 5000);
        connection.assertBuffer("build --target ");

        connection.read(Key.CTRL_C);

        console.stop();
    }

    @Test
    public void testSubCommandModeChildExecution() throws Exception {
        TestConnection connection = new TestConnection();
        BuildSubCommand.lastTarget = null;
        BuildSubCommand.executed = false;
        AtomicReference<CountDownLatch> latchRef = new AtomicReference<>(new CountDownLatch(1));

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(AppGroupCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .commandExecutionListener((line, result, durationMs) -> latchRef.get().countDown())
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // Enter sub-command mode
        connection.read("app" + Config.getLineSeparator());
        assertTrue("app should complete", latchRef.get().await(5, TimeUnit.SECONDS));
        connection.clearOutputBuffer();

        // Execute child command
        latchRef.set(new CountDownLatch(1));
        connection.read("build --target release" + Config.getLineSeparator());
        assertTrue("build should complete", latchRef.get().await(5, TimeUnit.SECONDS));

        assertTrue("build command should have executed", BuildSubCommand.executed);
        assertTrue("target should be 'release'", "release".equals(BuildSubCommand.lastTarget));

        connection.read("exit" + Config.getLineSeparator());
        console.stop();
    }

    // --- Test commands ---

    @CommandDefinition(name = "app", description = "Application manager", groupCommands = { BuildSubCommand.class,
            DeploySubCommand.class })
    public static class AppGroupCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            invocation.enterSubCommandMode(this);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "build", description = "Build the project", generateHelp = true)
    public static class BuildSubCommand implements Command<CommandInvocation> {
        static volatile boolean executed;
        static volatile String lastTarget;

        @Option(name = "target", description = "Build target")
        private String target;

        @Option(name = "clean", hasValue = false, description = "Clean before build")
        private boolean clean;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            executed = true;
            lastTarget = target;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "deploy", description = "Deploy the application", generateHelp = true)
    public static class DeploySubCommand implements Command<CommandInvocation> {
        @Option(name = "environment", allowedValues = { "dev", "staging", "prod" }, description = "Target environment")
        private String environment;

        @Option(name = "force", hasValue = false, description = "Force deployment")
        private boolean force;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    // --- Nested sub-command mode tests ---

    @Test
    public void testNestedSubCommandMode() throws Exception {
        TestConnection connection = new TestConnection();
        NestedLeafCmd.executed = false;
        NestedLeafCmd.lastValue = null;
        AtomicReference<CountDownLatch> latchRef = new AtomicReference<>(new CountDownLatch(1));

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(TopGroupCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .commandExecutionListener((line, result, durationMs) -> latchRef.get().countDown())
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // Enter top-level sub-command mode
        connection.read("top" + Config.getLineSeparator());
        assertTrue("top should complete", latchRef.get().await(5, TimeUnit.SECONDS));

        // Enter nested sub-command mode
        latchRef.set(new CountDownLatch(1));
        connection.read("mid" + Config.getLineSeparator());
        assertTrue("mid should complete", latchRef.get().await(5, TimeUnit.SECONDS));

        // Execute leaf command within nested context
        latchRef.set(new CountDownLatch(1));
        connection.read("leaf --value hello" + Config.getLineSeparator());
        assertTrue("leaf should complete", latchRef.get().await(5, TimeUnit.SECONDS));

        String output = connection.getOutputBuffer();
        assertTrue("should enter top mode, got: " + output, output.contains("Entering top mode"));
        assertTrue("should enter mid mode, got: " + output, output.contains("Entering mid mode"));
        assertTrue("leaf should have executed", NestedLeafCmd.executed);
        assertEquals("hello", NestedLeafCmd.lastValue);

        // Exit nested mode back to top
        connection.read("exit" + Config.getLineSeparator());

        // Exit top mode
        connection.read("exit" + Config.getLineSeparator());

        console.stop();
    }

    @Test
    public void testExitSubCommandModeWithDotDot() throws Exception {
        TestConnection connection = new TestConnection();
        AtomicReference<CountDownLatch> latchRef = new AtomicReference<>(new CountDownLatch(1));

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(AppGroupCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .commandExecutionListener((line, result, durationMs) -> latchRef.get().countDown())
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // Enter sub-command mode
        connection.read("app" + Config.getLineSeparator());
        assertTrue("app should complete", latchRef.get().await(5, TimeUnit.SECONDS));
        assertTrue(connection.getOutputBuffer().contains("Entering app mode"));
        connection.clearOutputBuffer();

        // Exit with ".." — readline processes sequentially from the input pipe,
        // so the next "app" command will be processed after ".." exits sub-command mode
        connection.read(".." + Config.getLineSeparator());

        // Should be back at the main prompt — verify by typing a top-level command
        connection.clearOutputBuffer();
        latchRef.set(new CountDownLatch(1));
        connection.read("app" + Config.getLineSeparator());
        assertTrue("re-enter app should complete", latchRef.get().await(5, TimeUnit.SECONDS));
        assertTrue("should re-enter app mode", connection.getOutputBuffer().contains("Entering app mode"));

        connection.read("exit" + Config.getLineSeparator());
        console.stop();
    }

    // --- Nested command definitions ---

    @CommandDefinition(name = "top", description = "Top level", groupCommands = { MidGroupCommand.class })
    public static class TopGroupCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            invocation.enterSubCommandMode(this);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mid", description = "Mid level", groupCommands = { NestedLeafCmd.class })
    public static class MidGroupCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            invocation.enterSubCommandMode(this);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "leaf", description = "Leaf command", generateHelp = true)
    public static class NestedLeafCmd implements Command<CommandInvocation> {
        static volatile boolean executed;
        static volatile String lastValue;

        @Option(name = "value", description = "A value")
        private String value;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            executed = true;
            lastValue = value;
            return CommandResult.SUCCESS;
        }
    }
}
