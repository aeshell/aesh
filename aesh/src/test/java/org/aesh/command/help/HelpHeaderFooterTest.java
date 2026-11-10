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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.DocFormat;
import org.aesh.command.HelpContext;
import org.aesh.command.HelpEntry;
import org.aesh.command.HelpFeature;
import org.aesh.command.HelpSectionProvider;
import org.aesh.command.Toggle;
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
    public void testHeaderBeforeSynopsisWhenRequested() throws Exception {
        String help = help(new HeaderFirstCommand());

        int description = help.indexOf("List threads first");
        int header = help.indexOf("Shows the threads of header-first.");
        int usage = help.indexOf("Usage: header-first");
        int options = help.indexOf("Options:");
        // description, then header, then usage, then options
        assertTrue(help, description >= 0 && description < header && header < usage && usage < options);
    }

    @Test
    public void testHeaderAppendedWhenNoBlankLine() throws Exception {
        // Minimal help has no blank line after the usage, so the default
        // placement appends the header at the end instead of injecting it
        String help = help(new MinimalCommand());

        int usage = help.indexOf("Usage: minimal");
        int header = help.indexOf("Minimal header");
        assertTrue(help, usage >= 0 && header > usage);
    }

    @Test
    public void testFleetDefaultPutsHeaderFirst() throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        builder.setHelpDefaults(EnumSet.of(HelpFeature.HEADER_BEFORE_SYNOPSIS));
        CommandLineParser<CommandInvocation> parser = builder.create(new ThreadDumpCommand()).getParser();
        parser.updateAnsiMode(false);
        String help = parser.printHelp();

        int usage = help.indexOf("Usage: thread-dump");
        int header = help.indexOf("Shows the threads of thread-dump.");
        assertTrue("fleet default puts header before usage, got:\n" + help, usage >= 0 && header >= 0 && header < usage);
    }

    @Test
    public void testChildInheritsHeaderBeforeSynopsis() throws Exception {
        CommandLineParser<CommandInvocation> group = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new HeaderFirstGroup()).getParser();
        CommandLineParser<CommandInvocation> child = null;
        for (CommandLineParser<CommandInvocation> c : group.getAllChildParsers()) {
            if (c.getProcessedCommand().name().equals("header-child"))
                child = c;
        }
        assertTrue("group should have a header-child", child != null);
        child.updateAnsiMode(false);
        String help = child.printHelp();

        int usage = help.indexOf("Usage:");
        int header = help.indexOf("Child header");
        assertTrue("inherited toggle puts header before usage, got:\n" + help, usage >= 0 && header >= 0 && header < usage);
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

    @Test
    public void testSharedProviderSeesCommandContext() throws Exception {
        // One provider class serves two commands, keying content off the context
        String alpha = help(new BundleAlphaCommand());
        assertTrue(alpha, alpha.contains("Header for bundle-alpha"));

        CommandLineParser<CommandInvocation> group = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new BundleGroup()).getParser();
        CommandLineParser<CommandInvocation> child = null;
        for (CommandLineParser<CommandInvocation> c : group.getAllChildParsers()) {
            if (c.getProcessedCommand().name().equals("bundle-child"))
                child = c;
        }
        assertTrue("group should have a bundle-child", child != null);
        child.updateAnsiMode(false);
        String childHelp = child.printHelp();
        assertTrue(childHelp, childHelp.contains("Header for bundle-group bundle-child"));
    }

    @Test
    public void testContextCarriesFormatInDocumentation() throws Exception {
        String help = help(new FormatCommand());
        assertTrue("context path is used for terminal help", help.contains("Format is "));

        String doc = DocumentationGenerator.builder()
                .commandClass(FormatCommand.class)
                .format(DocFormat.MARKDOWN)
                .generateSingle();
        assertTrue(doc, doc.contains("Format is MARKDOWN"));
    }

    private static String help(Command<CommandInvocation> command) throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>().create(command)
                .getParser();
        parser.updateAnsiMode(false);
        return parser.printHelp();
    }

    @CommandDefinition(name = "header-first", description = "List threads first", generateHelp = true, headerBeforeSynopsis = Toggle.ON, header = "Shows the threads of ${COMMAND-NAME}.")
    public static class HeaderFirstCommand implements Command<CommandInvocation> {
        @Option(hasValue = false, description = "Include the stack trace")
        boolean trace;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "minimal", description = "Minimal", header = "Minimal header")
    public static class MinimalCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "header-child", description = "Inheriting child", generateHelp = true, header = "Child header")
    public static class HeaderChildCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "header-group", description = "Header group", generateHelp = true, headerBeforeSynopsis = Toggle.ON, groupCommands = {
            HeaderChildCommand.class })
    public static class HeaderFirstGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
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

    /**
     * One provider serving many commands: content is keyed off the render
     * context instead of needing a subclass per command (#750).
     */
    public static class BundleProvider implements HelpSectionProvider {
        @Override
        public Map<String, List<HelpEntry>> getAdditionalSections() {
            return Collections.emptyMap();
        }

        @Override
        public String getHeader(HelpContext context) {
            return "Header for " + context.fullName();
        }
    }

    @CommandDefinition(name = "bundle-alpha", description = "Bundled alpha", generateHelp = true, helpSectionProvider = BundleProvider.class)
    public static class BundleAlphaCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "bundle-child", description = "Bundled child", generateHelp = true, helpSectionProvider = BundleProvider.class)
    public static class BundleChildCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "bundle-group", description = "Bundled group", generateHelp = true, groupCommands = {
            BundleChildCommand.class })
    public static class BundleGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class FormatProvider implements HelpSectionProvider {
        @Override
        public Map<String, List<HelpEntry>> getAdditionalSections() {
            return Collections.emptyMap();
        }

        @Override
        public String getHeader(HelpContext context) {
            return "Format is " + context.format();
        }
    }

    @CommandDefinition(name = "fmt", description = "Format in context", generateHelp = true, helpSectionProvider = FormatProvider.class)
    public static class FormatCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
