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
package org.aesh.command.alias;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.ReadlineConsole;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for the aesh alias subsystem: AeshAliasManager, AliasCommand, UnAliasCommand.
 *
 * @author Aesh team
 */
public class AeshAliasTest {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    // ========== AeshAliasManager unit tests ==========

    @Test
    public void testAeshAliasManagerBlocksCommandNameConflict() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(FooCommand.class)
                .create();

        File aliasFile = new File(tempDir.getRoot(), "aliases");
        AeshAliasManager manager = new AeshAliasManager(aliasFile, false, registry);

        // "foo" conflicts with the registered FooCommand
        assertFalse("Should reject alias that conflicts with command name",
                manager.verifyNoNewAliasConflict("foo"));

        // "myalias" does not conflict
        assertTrue("Should allow non-conflicting alias name",
                manager.verifyNoNewAliasConflict("myalias"));
    }

    @Test
    public void testAeshAliasManagerAddAndRetrieve() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(FooCommand.class)
                .create();

        File aliasFile = new File(tempDir.getRoot(), "aliases");
        AeshAliasManager manager = new AeshAliasManager(aliasFile, false, registry);

        // Add a valid alias
        Optional<String> result = manager.addAlias("alias ll='ls -la'");
        assertFalse("Should succeed without error message", result.isPresent());

        // Retrieve it
        Optional<?> alias = manager.getAlias("ll");
        assertTrue("Alias should exist", alias.isPresent());

        // Try to add alias conflicting with command name
        result = manager.addAlias("alias foo='bar'");
        assertTrue("Should return error for conflicting alias", result.isPresent());
        assertTrue(result.get().contains("foo"));
    }

    @Test
    public void testAeshAliasManagerPersistence() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(FooCommand.class)
                .create();

        File aliasFile = new File(tempDir.getRoot(), "aliases_persist");
        AeshAliasManager manager = new AeshAliasManager(aliasFile, true, registry);

        manager.addAlias("alias ll='ls -la'");
        manager.addAlias("alias gs='git status'");

        // Verify in-memory state
        assertTrue("ll should exist in memory", manager.getAlias("ll").isPresent());
        assertTrue("gs should exist in memory", manager.getAlias("gs").isPresent());

        manager.persist();
        assertTrue("Alias file should exist after persist", aliasFile.isFile());

        // Verify file content is readable and correctly formatted
        List<String> lines = java.nio.file.Files.readAllLines(aliasFile.toPath());
        assertEquals("File should have 2 aliases", 2, lines.size());
        assertTrue("File should contain gs", lines.stream().anyMatch(l -> l.contains("gs")));
        assertTrue("File should contain ll", lines.stream().anyMatch(l -> l.contains("ll")));
        // Each line should be in 'alias name=...' format
        for (String line : lines) {
            assertTrue("Each line should start with 'alias '", line.startsWith("alias "));
        }
    }

    @Test
    public void testAeshAliasManagerRemove() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(FooCommand.class)
                .create();

        File aliasFile = new File(tempDir.getRoot(), "aliases");
        AeshAliasManager manager = new AeshAliasManager(aliasFile, false, registry);

        manager.addAlias("alias ll='ls -la'");
        assertTrue(manager.getAlias("ll").isPresent());

        String error = manager.removeAlias("unalias ll");
        assertTrue("Should remove without error", error == null || error.isEmpty());
        assertFalse("ll should be gone", manager.getAlias("ll").isPresent());
    }

    @Test
    public void testAeshAliasManagerRemoveNonExistent() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(FooCommand.class)
                .create();

        File aliasFile = new File(tempDir.getRoot(), "aliases");
        AeshAliasManager manager = new AeshAliasManager(aliasFile, false, registry);

        String error = manager.removeAlias("unalias nonexistent");
        assertTrue("Should return error for nonexistent alias", error != null && !error.isEmpty());
    }

    @Test
    public void testAeshAliasManagerPrintAll() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(FooCommand.class)
                .create();

        File aliasFile = new File(tempDir.getRoot(), "aliases");
        AeshAliasManager manager = new AeshAliasManager(aliasFile, false, registry);

        manager.addAlias("alias ll='ls -la'");
        manager.addAlias("alias gs='git status'");

        String output = manager.printAllAliases();
        assertTrue("Should contain ll alias", output.contains("ll"));
        assertTrue("Should contain gs alias", output.contains("gs"));
        assertTrue("Should contain ls -la", output.contains("ls -la"));
    }

    @Test
    public void testAeshAliasManagerFindMatching() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(FooCommand.class)
                .create();

        File aliasFile = new File(tempDir.getRoot(), "aliases");
        AeshAliasManager manager = new AeshAliasManager(aliasFile, false, registry);

        manager.addAlias("alias ll='ls -la'");
        manager.addAlias("alias la='ls -A'");
        manager.addAlias("alias gs='git status'");

        List<String> matches = manager.findAllMatchingNames("l");
        assertEquals(2, matches.size());
        assertTrue(matches.contains("ll"));
        assertTrue(matches.contains("la"));

        matches = manager.findAllMatchingNames("g");
        assertEquals(1, matches.size());
        assertTrue(matches.contains("gs"));

        matches = manager.findAllMatchingNames("z");
        assertEquals(0, matches.size());
    }

    // ========== Integration tests with ReadlineConsole ==========

    @Test
    public void testAliasCommandViaConsole() throws Exception {
        TestConnection connection = new TestConnection();

        ReadlineConsole console = buildConsole(connection);
        console.start();

        // Define an alias
        connection.read("alias ll='ls -la'" + Config.getLineSeparator());
        Thread.sleep(100);

        // List aliases — should show ll
        connection.clearOutputBuffer();
        connection.read("alias" + Config.getLineSeparator());
        Thread.sleep(100);
        String output = connection.getOutputBuffer();
        assertTrue("alias listing should contain ll", output.contains("ll"));
        assertTrue("alias listing should contain ls -la", output.contains("ls -la"));

        console.stop();
    }

    @Test
    public void testUnaliasCommandViaConsole() throws Exception {
        TestConnection connection = new TestConnection();

        ReadlineConsole console = buildConsole(connection);
        console.start();

        // Define an alias
        connection.read("alias myalias='echo hello'" + Config.getLineSeparator());
        Thread.sleep(100);

        // Remove it
        connection.read("unalias myalias" + Config.getLineSeparator());
        Thread.sleep(100);

        // List aliases — should be empty or not contain myalias
        connection.clearOutputBuffer();
        connection.read("alias" + Config.getLineSeparator());
        Thread.sleep(100);
        String output = connection.getOutputBuffer();
        assertFalse("myalias should be removed", output.contains("myalias"));

        console.stop();
    }

    // ========== Helpers ==========

    private ReadlineConsole buildConsole(TestConnection connection) throws IOException, CommandRegistryException {
        File aliasFile = new File(tempDir.getRoot(), "test_aliases");

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(AeshCommandRegistryBuilder.<CommandInvocation> builder()
                        .command(FooCommand.class).create())
                .aliasFile(aliasFile)
                .connection(connection)
                .setPersistExport(false)
                .persistAlias(false)
                .logging(true)
                .build();

        return new ReadlineConsole(settings);
    }

    // ========== Test commands ==========

    @CommandDefinition(name = "foo", description = "A test command")
    public static class FooCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            invocation.println("foo executed");
            return CommandResult.SUCCESS;
        }
    }
}
