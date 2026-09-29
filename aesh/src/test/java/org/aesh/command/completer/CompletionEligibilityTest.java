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
package org.aesh.command.completer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ParsedCommand;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionVisibility;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.console.DefaultAeshContext;
import org.aesh.terminal.formatting.TerminalString;
import org.junit.Test;

/**
 * Completion eligibility must apply one consistent rule — visibility,
 * activation, already-set and exclusive — across the inherited, bare,
 * prefixed and suggestion paths (#658).
 */
public class CompletionEligibilityTest {

    public static class NeverActive implements OptionActivator {
        @Override
        public boolean isActivated(ParsedCommand parsedCommand) {
            return false;
        }
    }

    public static class AlwaysActive implements OptionActivator {
        @Override
        public boolean isActivated(ParsedCommand parsedCommand) {
            return true;
        }
    }

    @CommandDefinition(name = "leaf", description = "leaf")
    public static class LeafCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "root", description = "root", groupCommands = { LeafCommand.class })
    public static class RootCommand implements Command<CommandInvocation> {
        @Option(name = "gated", description = "gated", inherited = true, activator = NeverActive.class)
        String gated;

        @Option(name = "open", description = "open", inherited = true, activator = AlwaysActive.class)
        String open;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "bare", description = "bare names")
    public static class BareCommand implements Command<CommandInvocation> {
        @Option(name = "secret", description = "secret", acceptNameWithoutDashes = true, visibility = OptionVisibility.HIDDEN)
        String secret;

        @Option(name = "staging", description = "staging", acceptNameWithoutDashes = true)
        String staging;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    private static List<String> complete(CommandLineParser<CommandInvocation> parser, String buffer)
            throws Exception {
        InvocationProviders providers = SettingsBuilder.builder().build().invocationProviders();
        AeshCompleteOperation operation = new AeshCompleteOperation(new DefaultAeshContext(),
                buffer, buffer.length());
        parser.complete(operation, providers);
        List<String> candidates = new ArrayList<>();
        for (TerminalString candidate : operation.getCompletionCandidates())
            candidates.add(candidate.getCharacters());
        return candidates;
    }

    private static CommandLineParser<CommandInvocation> parserFor(Object command) throws Exception {
        return new AeshCommandContainerBuilder<CommandInvocation>().create(
                (Command<CommandInvocation>) command).getParser();
    }

    @Test
    public void testDeactivatedInheritedOptionNotSuggested() throws Exception {
        CommandLineParser<CommandInvocation> parser = parserFor(new RootCommand());

        List<String> candidates = complete(parser, "root leaf --g");

        assertFalse("Deactivated inherited option must not complete, got: " + candidates,
                candidates.contains("--gated"));
    }

    @Test
    public void testActivatedInheritedOptionSuggested() throws Exception {
        CommandLineParser<CommandInvocation> parser = parserFor(new RootCommand());

        List<String> candidates = complete(parser, "root leaf --o");

        assertTrue("Activated inherited option must complete, got: " + candidates,
                candidates.contains("--open"));
    }

    @Test
    public void testHiddenBareOptionNotSuggested() throws Exception {
        CommandLineParser<CommandInvocation> parser = parserFor(new BareCommand());

        List<String> candidates = complete(parser, "bare sec");

        assertFalse("Hidden bare option must not complete, got: " + candidates,
                candidates.contains("secret"));
    }

    @Test
    public void testVisibleBareOptionSuggested() throws Exception {
        CommandLineParser<CommandInvocation> parser = parserFor(new BareCommand());

        List<String> candidates = complete(parser, "bare st");

        assertTrue("Visible bare option must complete, got: " + candidates,
                candidates.contains("staging"));
    }
}
