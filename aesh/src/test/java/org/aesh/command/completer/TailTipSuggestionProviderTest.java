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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command.completer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.impl.completer.TailTipSuggestionProvider;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.junit.Test;

/**
 * Tests for {@link TailTipSuggestionProvider}.
 */
public class TailTipSuggestionProviderTest {

    @Test
    public void testNullAndEmptyBufferReturnsNull() throws Exception {
        TailTipSuggestionProvider<CommandInvocation> provider = createProvider();
        assertNull(provider.suggest(null));
        assertNull(provider.suggest(""));
    }

    @Test
    public void testMidWordReturnsNull() throws Exception {
        // No trailing space — user is mid-word, let auto-suggest handle it
        TailTipSuggestionProvider<CommandInvocation> provider = createProvider();
        assertNull(provider.suggest("dep"));
        assertNull(provider.suggest("deploy --for"));
    }

    @Test
    public void testCommandNameOnlyShowsAllParams() throws Exception {
        TailTipSuggestionProvider<CommandInvocation> provider = createProvider();
        String tip = provider.suggest("deploy ");
        assertNotNull("Should show tail tip after command name", tip);

        // Should contain option names and the argument
        // Note: synopsis uses short name -e (not --environment) when shortName is set
        assertTrue("Should contain -e: " + tip, tip.contains("-e"));
        assertTrue("Should contain --force: " + tip, tip.contains("--force"));
        assertTrue("Should contain <app>: " + tip, tip.contains("<app>"));
    }

    @Test
    public void testConsumedOptionNotShown() throws Exception {
        TailTipSuggestionProvider<CommandInvocation> provider = createProvider();
        String tip = provider.suggest("deploy --force ");
        assertNotNull("Should show tail tip after option", tip);

        // --force was consumed, should not appear in tip
        assertTrue("Should NOT contain --force: " + tip, !tip.contains("--force"));
        // Remaining options should still appear
        assertTrue("Should contain -e: " + tip, tip.contains("-e"));
        assertTrue("Should contain <app>: " + tip, tip.contains("<app>"));
    }

    @Test
    public void testConsumedValueOptionNotShown() throws Exception {
        TailTipSuggestionProvider<CommandInvocation> provider = createProvider();
        String tip = provider.suggest("deploy -e prod ");
        assertNotNull("Should show tail tip", tip);

        assertTrue("Should NOT contain -e: " + tip, !tip.contains("-e"));
        assertTrue("Should contain --force: " + tip, tip.contains("--force"));
        assertTrue("Should contain <app>: " + tip, tip.contains("<app>"));
    }

    @Test
    public void testAllOptionsConsumedShowsOnlyArgument() throws Exception {
        TailTipSuggestionProvider<CommandInvocation> provider = createProvider();
        String tip = provider.suggest("deploy --force -e prod ");
        assertNotNull("Should show tail tip with argument", tip);

        assertTrue("Should NOT contain --force: " + tip, !tip.contains("--force"));
        assertTrue("Should NOT contain -e: " + tip, !tip.contains("-e"));
        assertTrue("Should contain <app>: " + tip, tip.contains("<app>"));
    }

    @Test
    public void testUnknownCommandReturnsNull() throws Exception {
        TailTipSuggestionProvider<CommandInvocation> provider = createProvider();
        assertNull(provider.suggest("nonexistent "));
    }

    @Test
    public void testGroupCommandShowsSubcommands() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(GitCmd.class)
                .create();
        TailTipSuggestionProvider<CommandInvocation> provider = new TailTipSuggestionProvider<>(registry);

        String tip = provider.suggest("git ");
        assertNotNull("Should show tail tip for group command", tip);
        // Group command should show [COMMAND] placeholder
        assertTrue("Should contain [COMMAND]: " + tip, tip.contains("[COMMAND]"));
    }

    @Test
    public void testGroupSubcommandShowsRemainingOptions() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(GitCmd.class)
                .create();
        TailTipSuggestionProvider<CommandInvocation> provider = new TailTipSuggestionProvider<>(registry);

        String tip = provider.suggest("git commit ");
        assertNotNull("Should show tail tip for subcommand", tip);
        assertTrue("Should contain --message: " + tip, tip.contains("--message"));
    }

    @Test
    public void testCachingReturnsSameResult() throws Exception {
        TailTipSuggestionProvider<CommandInvocation> provider = createProvider();
        String tip1 = provider.suggest("deploy ");
        String tip2 = provider.suggest("deploy ");
        assertEquals("Cached result should be identical", tip1, tip2);
    }

    @Test
    public void testCacheInvalidatedOnBufferChange() throws Exception {
        TailTipSuggestionProvider<CommandInvocation> provider = createProvider();
        String tip1 = provider.suggest("deploy ");
        String tip2 = provider.suggest("deploy --force ");

        // After --force is consumed, the tip should be different
        assertTrue("--force should be in first tip", tip1.contains("--force"));
        assertTrue("--force should NOT be in second tip", !tip2.contains("--force"));
    }

    // --- Helpers ---

    private TailTipSuggestionProvider<CommandInvocation> createProvider() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(DeployCmd.class)
                .create();
        return new TailTipSuggestionProvider<>(registry);
    }

    // --- Test commands ---

    @CommandDefinition(name = "deploy", description = "Deploy an application")
    public static class DeployCmd implements Command<CommandInvocation> {
        @Option(shortName = 'e', description = "Target environment")
        public String environment;

        @Option(hasValue = false, description = "Force deploy")
        public boolean force;

        @Argument(description = "Application name")
        public String app;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "commit", description = "Commit changes")
    public static class GitCommitCmd implements Command<CommandInvocation> {
        @Option(description = "Commit message")
        public String message;

        @Option(hasValue = false, description = "Stage all")
        public boolean all;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "git", description = "Git", groupCommands = { GitCommitCmd.class })
    public static class GitCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }
}
