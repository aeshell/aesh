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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.parser.AeshCommandLineParser;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.junit.Test;

/**
 * Generated {@code --help}/{@code --version} yield to explicit user options
 * instead of failing the build with a model dump (#770).
 */
public class GeneratedOptionClashTest {

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testExplicitShortVKeepsLongOnlyVersion() throws Exception {
        // The issue's case: -v/--verbose plus version="1.0" must build.
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new VerboseTool()).getParser();
        parser.updateAnsiMode(false);

        ProcessedOption version = parser.getProcessedCommand().findLongOptionNoActivatorCheck("version");
        assertNotNull("--version survives long-only", version);
        assertNull("generated -v yields to the explicit one", version.shortName());

        ProcessedOption verbose = parser.getProcessedCommand().findLongOptionNoActivatorCheck("verbose");
        assertNotNull(verbose);
        assertEquals("v", verbose.shortName());

        // -v still parses to the explicit option
        parser.parse("tool -v");
        assertNotNull(verbose.getValue());

        // --version still resolves as generated
        CommandLineParser<CommandInvocation> versionParser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new VerboseTool()).getParser();
        versionParser.updateAnsiMode(false);
        versionParser.parse("tool --version");
        assertTrue(versionParser.getProcessedCommand().isGenerateVersionOptionSet());

        // Help shows --version without a short flag (skip the synopsis line).
        // The short form renders as "-v, --version"; "--version" alone
        // trivially contains the substring "-v".
        String help = parser.printHelp();
        for (String line : help.split("\\r?\\n")) {
            if (line.contains("--version") && !line.contains("Usage:")) {
                assertTrue("Version line must be long-only, got: " + line,
                        line.trim().startsWith("--version"));
                assertFalse("No short -v on the version line, got: " + line,
                        line.contains("-v,"));
            }
        }
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testExplicitLongVersionWins() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new CustomVersionTool()).getParser();
        parser.updateAnsiMode(false);

        // Exactly one --version: the user's own option
        int count = 0;
        for (ProcessedOption opt : parser.getProcessedCommand().getOptions()) {
            if ("version".equals(opt.name()))
                count++;
        }
        assertEquals("user --version replaces the generated one", 1, count);
        assertEquals("custom", parser.getProcessedCommand().findLongOptionNoActivatorCheck("version")
                .getFieldName());
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testExplicitShortHWinsWithoutHelp() throws Exception {
        // Generated --help keeps its skip-when-taken behavior on this path.
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new HostTool()).getParser();
        parser.updateAnsiMode(false);

        assertNull("no generated --help when -h is taken",
                parser.getProcessedCommand().findLongOptionNoActivatorCheck("help"));
        assertFalse(parser.printHelp().contains("Display this help and exit"));
    }

    @Test
    public void testBuilderPathAgreesOnLongOnlyVersion() throws Exception {
        // ProcessedCommandBuilder generates after the user options; the
        // outcome must match the container path.
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder
                .builder();
        pb.name("tool").description("Tool").generateHelp(true).version("1.0");
        pb.addOption(ProcessedOptionBuilder.builder().shortName('v').name("verbose")
                .description("Verbose output").type(boolean.class).hasValue(false).build());
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> pc = pb.create();

        ProcessedOption version = pc.findLongOptionNoActivatorCheck("version");
        assertNotNull(version);
        assertNull(version.shortName());

        AeshCommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pc);
        clp.updateAnsiMode(false);
        assertTrue(clp.printHelp().contains("--version"));
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testUserDuplicateNamesBothSides() {
        try {
            new AeshCommandContainerBuilder<CommandInvocation>().create(new DuplicateShortTool());
            fail("Expected duplicate short name to fail");
        } catch (CommandLineParserException e) {
            assertTrue("names the new option, got: " + e.getMessage(),
                    e.getMessage().contains("'second'"));
            assertTrue("names the existing option, got: " + e.getMessage(),
                    e.getMessage().contains("'first'"));
            assertFalse("no model dump, got: " + e.getMessage(),
                    e.getMessage().contains("ProcessedCommand{"));
        }
    }

    @CommandDefinition(name = "tool", description = "Tool", generateHelp = true, version = "1.0")
    public static class VerboseTool implements Command<CommandInvocation> {
        @Option(shortName = 'v', name = "verbose", description = "Verbose output", hasValue = false)
        boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "tool", description = "Tool", generateHelp = true, version = "1.0")
    public static class CustomVersionTool implements Command<CommandInvocation> {
        @Option(name = "version", description = "Custom version output")
        String custom;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "tool", description = "Tool", generateHelp = true, version = "1.0")
    public static class HostTool implements Command<CommandInvocation> {
        @Option(shortName = 'h', name = "host", description = "Remote host")
        String host;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "tool", description = "Tool")
    public static class DuplicateShortTool implements Command<CommandInvocation> {
        @Option(shortName = 'x', name = "first", description = "First")
        String first;
        @Option(shortName = 'x', name = "second", description = "Second")
        String second;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
