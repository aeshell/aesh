/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.aesh.command.help;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.DocFormat;
import org.aesh.command.HelpEntry;
import org.aesh.command.HelpSectionProvider;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.util.doc.DocumentationGenerator;
import org.junit.Test;

/**
 * The {@code header} and {@code footer} of {@code @CommandDefinition} are shown in the help, one line per element,
 * unless a HelpSectionProvider gives its own.
 */
public class HelpHeaderFooterTest {

    @Test
    public void testFooterAtTheEnd() throws Exception {
        String help = help(new ThreadDumpCommand());
        String[] lines = help.split("\\R");

        assertEquals(help, "  thread-dump --trace", lines[lines.length - 1]);
        assertEquals(help, "  thread-dump", lines[lines.length - 2]);
        assertEquals(help, "Examples:", lines[lines.length - 3]);
        assertTrue(help, help.indexOf("--trace ") < help.indexOf("Examples:"));
    }

    @Test
    public void testHeaderAfterTheUsage() throws Exception {
        String help = help(new ThreadDumpCommand());

        int usage = help.indexOf("Usage: thread-dump");
        int header = help.indexOf("Shows the threads of thread-dump.");
        int options = help.indexOf("Options:");
        assertTrue(help, usage >= 0 && usage < header && header < options);
    }

    @Test
    public void testProviderTakesPrecedence() throws Exception {
        String help = help(new ProviderCommand());

        assertTrue(help, help.contains("Footer of the provider"));
        assertFalse(help, help.contains("Footer of the annotation"));
        // the provider gives no header, so the one of the annotation is shown
        assertTrue(help, help.contains("Header of the annotation"));
    }

    @Test
    public void testNoHeaderOrFooter() throws Exception {
        String help = help(new PlainCommand());

        assertFalse(help, help.contains("Examples:"));
        assertTrue(help, help.trim().endsWith("Output as JSON"));
    }

    @Test
    public void testFooterInTheDocumentation() throws Exception {
        String doc = DocumentationGenerator.builder()
                .commandClass(ThreadDumpCommand.class)
                .format(DocFormat.MARKDOWN)
                .generateSingle();

        assertTrue(doc, doc.contains("Shows the threads of thread-dump."));
        assertTrue(doc, doc.contains("  thread-dump --trace"));
    }

    @Test
    public void testCommandItselfAsProvider() throws Exception {
        String help = help(new SelfProvidedCommand());

        assertTrue(help, help.contains("Header of the command itself"));
        assertTrue(help, help.contains("Footer of the command itself"));
        assertTrue(help, help.contains("Self examples:"));
        assertTrue(help, help.contains("self-cmd --trace"));
    }

    @Test
    public void testExplicitProviderWinsOverCommandItself() throws Exception {
        String help = help(new SelfProvidedWithAnnotation());

        assertTrue(help, help.contains("Footer of the provider"));
        assertFalse(help, help.contains("Footer of the command itself"));
    }

    @Test
    public void testProgrammaticInstanceWinsOverCommandItself() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new SelfProvidedCommand()).getParser();
        parser.getProcessedCommand().setHelpSectionProvider(new FooterProvider());
        parser.updateAnsiMode(false);
        String help = parser.printHelp();

        assertTrue(help, help.contains("Footer of the provider"));
        assertFalse(help, help.contains("Footer of the command itself"));
    }

    @Test
    public void testCommandItselfInTheDocumentation() throws Exception {
        String doc = DocumentationGenerator.builder()
                .commandClass(SelfProvidedCommand.class)
                .format(DocFormat.MARKDOWN)
                .generateSingle();

        assertTrue(doc, doc.contains("Header of the command itself"));
        assertTrue(doc, doc.contains("Footer of the command itself"));
    }

    private static String help(Command<CommandInvocation> command) throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>().create(command)
                .getParser();
        parser.updateAnsiMode(false);
        return parser.printHelp();
    }

    @CommandDefinition(name = "thread-dump", description = "List threads", generateHelp = true, header = "Shows the threads of ${COMMAND-NAME}.", footer = {
            "Examples:", "  thread-dump", "  thread-dump --trace" })
    public static class ThreadDumpCommand implements Command<CommandInvocation> {
        @Option(hasValue = false, description = "Include the stack trace")
        boolean trace;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "provided", description = "With a provider", generateHelp = true, header = "Header of the annotation", footer = "Footer of the annotation", helpSectionProvider = FooterProvider.class)
    public static class ProviderCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "plain", description = "No header or footer", generateHelp = true)
    public static class PlainCommand implements Command<CommandInvocation> {
        @Option(hasValue = false, description = "Output as JSON")
        boolean json;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class FooterProvider implements HelpSectionProvider {
        @Override
        public Map<String, List<HelpEntry>> getAdditionalSections() {
            return Collections.emptyMap();
        }

        @Override
        public String getFooter() {
            return "Footer of the provider";
        }
    }

    @CommandDefinition(name = "self-cmd", description = "Provides its own help", generateHelp = true)
    public static class SelfProvidedCommand implements Command<CommandInvocation>, HelpSectionProvider {
        @Option(hasValue = false, description = "Include the stack trace")
        boolean trace;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }

        @Override
        public Map<String, List<HelpEntry>> getAdditionalSections() {
            return Collections.singletonMap("Self examples:",
                    Collections.singletonList(new HelpEntry("self-cmd --trace", "Trace it")));
        }

        @Override
        public String getHeader() {
            return "Header of the command itself";
        }

        @Override
        public String getFooter() {
            return "Footer of the command itself";
        }
    }

    @CommandDefinition(name = "self-annotated", description = "Self provider plus annotation", generateHelp = true, helpSectionProvider = FooterProvider.class)
    public static class SelfProvidedWithAnnotation implements Command<CommandInvocation>, HelpSectionProvider {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }

        @Override
        public Map<String, List<HelpEntry>> getAdditionalSections() {
            return Collections.emptyMap();
        }

        @Override
        public String getFooter() {
            return "Footer of the command itself";
        }
    }
}
