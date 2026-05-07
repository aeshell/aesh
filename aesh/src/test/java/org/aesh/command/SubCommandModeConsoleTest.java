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

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(AppGroupCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // Type "app" + enter to enter sub-command mode
        connection.read("app" + Config.getLineSeparator());
        Thread.sleep(200);

        String output = connection.getOutputBuffer();
        assertTrue("should show entering message", output.contains("Entering app mode"));
        connection.clearOutputBuffer();

        // Now in sub-command mode — type "build --help" + enter
        connection.read("build --help" + Config.getLineSeparator());
        Thread.sleep(200);

        output = connection.getOutputBuffer();
        assertTrue("help should contain command name 'build'", output.contains("build"));
        assertTrue("help should contain --target option", output.contains("target"));
        assertTrue("help should contain --clean option", output.contains("clean"));
        connection.clearOutputBuffer();

        // Type "deploy --help" + enter
        connection.read("deploy --help" + Config.getLineSeparator());
        Thread.sleep(200);

        output = connection.getOutputBuffer();
        assertTrue("deploy help should contain 'deploy'", output.contains("deploy"));
        assertTrue("deploy help should contain --environment", output.contains("environment"));
        connection.clearOutputBuffer();

        // Exit sub-command mode
        connection.read("exit" + Config.getLineSeparator());
        Thread.sleep(100);

        console.stop();
    }

    @Test
    public void testSubCommandModeCompletion() throws Exception {
        TestConnection connection = new TestConnection();

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(AppGroupCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // Enter sub-command mode
        connection.read("app" + Config.getLineSeparator());
        Thread.sleep(200);
        connection.clearOutputBuffer();

        // Tab-complete "bu" should complete to "build "
        connection.read("bu");
        connection.read(Key.CTRL_I);
        Thread.sleep(100);
        connection.assertBuffer("build ");

        // Clear and tab-complete "de" should complete to "deploy "
        connection.read(Key.CTRL_C);
        Thread.sleep(100);
        // After Ctrl+C in sub-command mode, we might exit — re-enter if needed
        String output = connection.getOutputBuffer();
        if (!output.contains("app")) {
            // Re-enter sub-command mode
            connection.read("app" + Config.getLineSeparator());
            Thread.sleep(200);
        }
        connection.clearOutputBuffer();

        connection.read("de");
        connection.read(Key.CTRL_I);
        Thread.sleep(100);
        connection.assertBuffer("deploy ");

        connection.read("exit" + Config.getLineSeparator());
        Thread.sleep(100);
        console.stop();
    }

    @Test
    public void testSubCommandModeOptionNameCompletion() throws Exception {
        TestConnection connection = new TestConnection();

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(AppGroupCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // Enter sub-command mode
        connection.read("app" + Config.getLineSeparator());
        Thread.sleep(200);
        connection.clearOutputBuffer();

        // Type "build --targ" + tab — should complete to "build --target=" without trailing space
        connection.read("build --targ");
        connection.read(Key.CTRL_I);
        Thread.sleep(100);
        connection.assertBuffer("build --target ");

        connection.read(Key.CTRL_C);
        Thread.sleep(100);

        console.stop();
    }

    @Test
    public void testSubCommandModeChildExecution() throws Exception {
        TestConnection connection = new TestConnection();
        BuildSubCommand.lastTarget = null;
        BuildSubCommand.executed = false;

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(AppGroupCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // Enter sub-command mode
        connection.read("app" + Config.getLineSeparator());
        Thread.sleep(200);
        connection.clearOutputBuffer();

        // Execute child command
        connection.read("build --target release" + Config.getLineSeparator());
        Thread.sleep(200);

        assertTrue("build command should have executed", BuildSubCommand.executed);
        assertTrue("target should be 'release'", "release".equals(BuildSubCommand.lastTarget));

        connection.read("exit" + Config.getLineSeparator());
        Thread.sleep(100);
        console.stop();
    }

    // --- Test commands ---

    @GroupCommandDefinition(name = "app", description = "Application manager", groupCommands = { BuildSubCommand.class,
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

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(TopGroupCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // Enter top-level sub-command mode
        connection.read("top" + Config.getLineSeparator());
        Thread.sleep(300);

        // Enter nested sub-command mode
        connection.read("mid" + Config.getLineSeparator());
        Thread.sleep(300);

        // Execute leaf command within nested context
        connection.read("leaf --value hello" + Config.getLineSeparator());
        Thread.sleep(300);

        String output = connection.getOutputBuffer();
        assertTrue("should enter top mode, got: " + output, output.contains("Entering top mode"));
        assertTrue("should enter mid mode, got: " + output, output.contains("Entering mid mode"));
        assertTrue("leaf should have executed", NestedLeafCmd.executed);
        assertEquals("hello", NestedLeafCmd.lastValue);

        // Exit nested mode back to top
        connection.read("exit" + Config.getLineSeparator());
        Thread.sleep(100);

        // Exit top mode
        connection.read("exit" + Config.getLineSeparator());
        Thread.sleep(100);

        console.stop();
    }

    @Test
    public void testExitSubCommandModeWithDotDot() throws Exception {
        TestConnection connection = new TestConnection();

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(AppGroupCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // Enter sub-command mode
        connection.read("app" + Config.getLineSeparator());
        Thread.sleep(200);
        assertTrue(connection.getOutputBuffer().contains("Entering app mode"));
        connection.clearOutputBuffer();

        // Exit with ".."
        connection.read(".." + Config.getLineSeparator());
        Thread.sleep(200);

        // Should be back at the main prompt — verify by typing a top-level command
        connection.clearOutputBuffer();
        connection.read("app" + Config.getLineSeparator());
        Thread.sleep(200);
        assertTrue("should re-enter app mode", connection.getOutputBuffer().contains("Entering app mode"));

        connection.read("exit" + Config.getLineSeparator());
        Thread.sleep(100);
        console.stop();
    }

    // --- Nested command definitions ---

    @GroupCommandDefinition(name = "top", description = "Top level", groupCommands = { MidGroupCommand.class })
    public static class TopGroupCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            invocation.enterSubCommandMode(this);
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "mid", description = "Mid level", groupCommands = { NestedLeafCmd.class })
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
