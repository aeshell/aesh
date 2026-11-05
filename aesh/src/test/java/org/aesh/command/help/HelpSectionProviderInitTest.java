/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.aesh.command.help;

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
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.util.doc.DocumentationGenerator;
import org.junit.Test;

/**
 * A HelpSectionProvider is told which command it renders for, so one generic provider can serve every command.
 */
public class HelpSectionProviderInitTest {

    @Test
    public void testOneProviderClassServesTwoCommands() throws Exception {
        String list = help(new ListCommand());
        String show = help(new ShowCommand());

        assertTrue(list, list.contains("Header of list"));
        assertFalse(list, list.contains("Header of show"));
        assertTrue(show, show.contains("Header of show"));
        assertTrue(show, show.contains("Footer of show"));
    }

    @Test
    public void testProviderInstanceIsInitialized() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new PlainCommand()).getParser();
        parser.getProcessedCommand().setHelpSectionProvider(new NameProvider());
        parser.updateAnsiMode(false);

        assertTrue(parser.printHelp().contains("Header of plain"));
    }

    @Test
    public void testDocumentationIsInitialized() throws Exception {
        String doc = DocumentationGenerator.builder()
                .commandClass(ShowCommand.class)
                .format(DocFormat.MARKDOWN)
                .generateSingle();

        assertTrue(doc, doc.contains("Header of show"));
        assertTrue(doc, doc.contains("Footer of show"));
    }

    private static String help(Command<CommandInvocation> command) throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>().create(command)
                .getParser();
        parser.updateAnsiMode(false);
        return parser.printHelp();
    }

    @CommandDefinition(name = "list", description = "List things", generateHelp = true, helpSectionProvider = NameProvider.class)
    public static class ListCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "show", description = "Show a thing", generateHelp = true, helpSectionProvider = NameProvider.class)
    public static class ShowCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "plain", description = "Plain", generateHelp = true)
    public static class PlainCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class NameProvider implements HelpSectionProvider {
        private String name;

        @Override
        public void init(ProcessedCommand<?, ?> command) {
            name = command.name();
        }

        @Override
        public Map<String, List<HelpEntry>> getAdditionalSections() {
            return Collections.emptyMap();
        }

        @Override
        public String getHeader() {
            return "Header of " + name;
        }

        @Override
        public String getFooter() {
            return "Footer of " + name;
        }
    }
}
