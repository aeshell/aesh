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
package org.aesh.command.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.parser.CompleteStatus;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionGroup;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.console.AeshContext;
import org.aesh.console.DefaultAeshContext;
import org.junit.Test;

/**
 * A long option followed by {@code =} must match exactly (primary name or
 * alias) before the boundary. Unknown suffixes such as
 * {@code --timeoutTypo=42} must report an unknown option instead of
 * populating the prefix-matched option (#650). Attached property keys for
 * {@code OptionGroup} ({@code --manifestFoo=Bar}) keep working.
 */
public class OptionEqualsBoundaryTest {

    private final InvocationProviders invocationProviders = new AeshInvocationProviders();

    @CommandDefinition(name = "deploy", description = "deploy")
    public static class TimeoutCommand implements Command<CommandInvocation> {
        @Option(name = "timeout", description = "timeout", aliases = { "to" })
        int timeout;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "overlap", description = "overlapping names")
    public static class OverlapCommand implements Command<CommandInvocation> {
        @Option(name = "time", description = "time")
        int time;
        @Option(name = "timeout", description = "timeout")
        int timeout;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "buildcmd", description = "group")
    public static class ManifestCommand implements Command<CommandInvocation> {
        @OptionGroup(shortName = 'D', name = "manifest", description = "entries")
        Map<String, String> manifest;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "helped", description = "helped", generateHelp = true)
    public static class HelpedCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    private AeshContext context() {
        return SettingsBuilder.builder().build().aeshContext();
    }

    private CommandLineParser<CommandInvocation> parserFor(Object command) throws Exception {
        return new AeshCommandContainerBuilder<CommandInvocation>().create(
                (Command<CommandInvocation>) command).getParser();
    }

    @Test
    public void testExactNameBeforeEqualsPopulates() throws Exception {
        TimeoutCommand cmd = new TimeoutCommand();
        CommandLineParser<CommandInvocation> parser = parserFor(cmd);

        parser.populateObject("deploy --timeout=42", invocationProviders, context(),
                CommandLineParser.Mode.VALIDATE);
        assertEquals(42, cmd.timeout);
    }

    @Test
    public void testAliasBeforeEqualsPopulates() throws Exception {
        TimeoutCommand cmd = new TimeoutCommand();
        CommandLineParser<CommandInvocation> parser = parserFor(cmd);

        parser.populateObject("deploy --to=42", invocationProviders, context(),
                CommandLineParser.Mode.VALIDATE);
        assertEquals(42, cmd.timeout);
    }

    @Test
    public void testUnknownSuffixBeforeEqualsIsRejected() throws Exception {
        TimeoutCommand cmd = new TimeoutCommand();
        CommandLineParser<CommandInvocation> parser = parserFor(cmd);

        try {
            parser.populateObject("deploy --timeoutTypo=42", invocationProviders, context(),
                    CommandLineParser.Mode.VALIDATE);
            fail("Expected unknown-option error for --timeoutTypo=42, got timeout=" + cmd.timeout);
        } catch (CommandLineParserException e) {
            assertTrue("Error must name the unknown input, got: " + e.getMessage(),
                    e.getMessage().contains("timeoutTypo"));
        }
    }

    @Test
    public void testAliasPrefixBeforeEqualsIsRejected() throws Exception {
        TimeoutCommand cmd = new TimeoutCommand();
        CommandLineParser<CommandInvocation> parser = parserFor(cmd);

        try {
            parser.populateObject("deploy --toTypo=42", invocationProviders, context(),
                    CommandLineParser.Mode.VALIDATE);
            fail("Expected unknown-option error for --toTypo=42");
        } catch (CommandLineParserException e) {
            assertTrue("Error must name the unknown input, got: " + e.getMessage(),
                    e.getMessage().contains("toTypo"));
        }
    }

    @Test
    public void testOverlappingNamesResolveExactly() throws Exception {
        OverlapCommand cmd = new OverlapCommand();
        CommandLineParser<CommandInvocation> parser = parserFor(cmd);

        parser.populateObject("overlap --time=5", invocationProviders, context(),
                CommandLineParser.Mode.VALIDATE);
        assertEquals(5, cmd.time);
        assertEquals(0, cmd.timeout);
    }

    @Test
    public void testEmptyValueAccepted() throws Exception {
        NameCommand cmd = new NameCommand();
        CommandLineParser<CommandInvocation> parser = parserFor(cmd);

        // Empty values are a value-handling concern, not a name-boundary one:
        // the name must resolve and the empty value must flow through.
        parser.populateObject("deploy --name=", invocationProviders, context(),
                CommandLineParser.Mode.VALIDATE);
        assertEquals("", cmd.name);
    }

    @CommandDefinition(name = "deploy", description = "deploy")
    public static class NameCommand implements Command<CommandInvocation> {
        @Option(name = "name", description = "name")
        String name;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testGroupAttachedKeyStillWorks() throws Exception {
        ManifestCommand cmd = new ManifestCommand();
        CommandLineParser<CommandInvocation> parser = parserFor(cmd);

        parser.populateObject("buildcmd --manifestFoo=Bar", invocationProviders, context(),
                CommandLineParser.Mode.VALIDATE);
        assertEquals("Bar", cmd.manifest.get("Foo"));

        parser.populateObject("buildcmd --manifest=Foo=Bar", invocationProviders, context(),
                CommandLineParser.Mode.VALIDATE);
        assertEquals("Bar", cmd.manifest.get("Foo"));
    }

    @Test
    public void testGeneratedHelpTypoIsRejected() throws Exception {
        HelpedCommand cmd = new HelpedCommand();
        CommandLineParser<CommandInvocation> parser = parserFor(cmd);

        try {
            parser.populateObject("helped --helpTypo=x", invocationProviders, context(),
                    CommandLineParser.Mode.VALIDATE);
            fail("Expected unknown-option error for --helpTypo=x");
        } catch (CommandLineParserException e) {
            assertTrue("Error must name the unknown input, got: " + e.getMessage(),
                    e.getMessage().contains("helpTypo"));
        }
    }

    @Test
    public void testCompletionTreatsTypoWithEqualsAsName() throws Exception {
        CommandLineParser<CommandInvocation> parser = parserFor(new TimeoutCommand());
        InvocationProviders ip = SettingsBuilder.builder().build().invocationProviders();

        // A typo before `=` no longer resolves to the prefix-matched option,
        // so completion treats it as a (partial) long name instead of a
        // value slot for --timeout.
        String buffer = "deploy --timeoutTypo=";
        AeshCompleteOperation co = new AeshCompleteOperation(new DefaultAeshContext(), buffer,
                buffer.length());
        parser.complete(co, ip);
        assertEquals(CompleteStatus.Status.LONG_OPTION,
                parser.getProcessedCommand().completeStatus().status());
    }

    @Test
    public void testCompletionDisambiguatesOverlappingNames() throws Exception {
        CommandLineParser<CommandInvocation> parser = parserFor(new OverlapCommand());
        InvocationProviders ip = SettingsBuilder.builder().build().invocationProviders();

        String buffer = "overlap --time";
        AeshCompleteOperation co = new AeshCompleteOperation(new DefaultAeshContext(), buffer,
                buffer.length());
        parser.complete(co, ip);
        // Long-name completion offers append-suffixes: "" completes --time
        // itself, "out" completes --timeout.
        assertTrue("Completion must offer --time, got: " + co.getFormattedCompletionCandidates(),
                co.getFormattedCompletionCandidates().contains(""));
        assertTrue("Completion must offer --timeout, got: " + co.getFormattedCompletionCandidates(),
                co.getFormattedCompletionCandidates().contains("out"));
    }
}
