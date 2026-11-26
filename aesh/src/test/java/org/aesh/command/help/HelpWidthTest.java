/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.aesh.command.help;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.junit.Test;

/**
 * Help width resolution: explicit {@code aesh.help.width} system property
 * wins, then the {@code COLUMNS} environment variable, then the live
 * terminal width, then the default. Everything clamps to a usable minimum.
 */
public class HelpWidthTest {

    @Test
    public void testSystemPropertyOverride() throws Exception {
        setWidth("120");
        try {
            assertEquals(120, helpWidth());
            assertEquals(120, ProcessedCommand.resolveHelpWidth());
        } finally {
            clearWidth();
        }
    }

    @Test
    public void testTinyWidthClamped() throws Exception {
        setWidth("10");
        try {
            assertEquals("tiny widths clamp up", 40, helpWidth());
        } finally {
            clearWidth();
        }
    }

    @Test
    public void testNonNumericWidthIgnored() throws Exception {
        setWidth("wide");
        try {
            int width = helpWidth();
            assertTrue("unparseable override falls through, got: " + width, width >= 40);
        } finally {
            clearWidth();
        }
    }

    @Test
    public void testWidthFloorWithoutOverrides() throws Exception {
        // Exercise the true no-override path even when the test JVM pins a
        // default width (see surefire systemPropertyVariables).
        String pinned = System.getProperty("aesh.help.width");
        System.clearProperty("aesh.help.width");
        try {
            assertTrue(helpWidth() >= 40);
        } finally {
            if (pinned != null)
                System.setProperty("aesh.help.width", pinned);
        }
    }

    @Test
    public void testHelpWrapsAtOverriddenWidth() throws Exception {
        setWidth("120");
        int wideLines;
        try {
            wideLines = help(new WideCommand()).split("\\R").length;
        } finally {
            clearWidth();
        }
        setWidth("50");
        int narrowLines;
        try {
            narrowLines = help(new WideCommand()).split("\\R").length;
        } finally {
            clearWidth();
        }
        assertTrue("narrow width wraps onto more lines: wide=" + wideLines + " narrow=" + narrowLines,
                narrowLines > wideLines);
    }

    @Test
    public void testThreadedWidthHonored() throws Exception {
        CommandLineParser<CommandInvocation> parser = parser(new WideCommand());
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> command = processed(parser);
        int resolved = command.helpWidth();
        assertEquals("threaded width must match resolving width",
                command.printHelp("wide", false, false), command.printHelp("wide", false, false, resolved));
        int narrowLines = command.printHelp("wide", false, false, 50).split("\\R").length;
        int wideLines = command.printHelp("wide", false, false, 120).split("\\R").length;
        assertTrue("threaded width must drive wrapping: narrow=" + narrowLines + " wide=" + wideLines,
                narrowLines > wideLines);

        CommandLineParser<CommandInvocation> synopsisParser = parser(new SynopsisCommand());
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> synopsis = processed(synopsisParser);
        int synopsisResolved = synopsis.helpWidth();
        assertEquals("threaded width must match resolving width",
                synopsis.printSynopsis("syn", false),
                synopsis.printSynopsis("syn", false, synopsisResolved));
        String narrowSynopsis = synopsis.printSynopsis("syn", false, 40);
        String wideSynopsis = synopsis.printSynopsis("syn", false, 120);
        assertTrue("threaded width must drive synopsis wrapping, narrow: " + narrowSynopsis
                + " wide: " + wideSynopsis, !narrowSynopsis.equals(wideSynopsis));
    }

    @Test
    public void testThreadedFormattedCommandWidthHonored() throws Exception {
        CommandLineParser<CommandInvocation> parser = parser(new WideGroup());
        CommandLineParser<CommandInvocation> child = parser.getChildParsers().get(0);
        String narrow = child.getFormattedCommand(4, 20, 60);
        String wide = child.getFormattedCommand(4, 20, 140);
        assertTrue("threaded width must drive child wrapping, got: " + narrow,
                !narrow.equals(wide));
    }

    private static int helpWidth() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new WideCommand()).getParser();
        return parser.getProcessedCommand().helpWidth();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static ProcessedCommand<Command<CommandInvocation>, CommandInvocation> processed(
            CommandLineParser<CommandInvocation> parser) {
        return (ProcessedCommand<Command<CommandInvocation>, CommandInvocation>) (ProcessedCommand) parser
                .getProcessedCommand();
    }

    private static CommandLineParser<CommandInvocation> parser(Command<CommandInvocation> command) throws Exception {
        return new AeshCommandContainerBuilder<CommandInvocation>().create(command).getParser();
    }

    private static String help(Command<CommandInvocation> command) throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>().create(command)
                .getParser();
        parser.updateAnsiMode(false);
        return parser.printHelp();
    }

    private static void setWidth(String value) {
        System.setProperty("aesh.help.width", value);
    }

    /**
     * The width pin surefire sets for the whole fork (see aesh/pom.xml,
     * {@code systemPropertyVariables}). Tests that override the width must
     * restore THIS value, not clear the property: clearing deletes the pin
     * for every later test class in the fork, and their help width then
     * follows the live terminal — wide on a developer's terminal, 80 on CI,
     * so width-sensitive assertions fail only locally.
     */
    private static final String PINNED_WIDTH = System.getProperty("aesh.help.width");

    private static void clearWidth() {
        if (PINNED_WIDTH != null)
            System.setProperty("aesh.help.width", PINNED_WIDTH);
        else
            System.clearProperty("aesh.help.width");
    }

    @CommandDefinition(name = "wide", description = "A command with a deliberately long description that must wrap onto several lines when the help width is narrow", generateHelp = true)
    public static class WideCommand implements Command<CommandInvocation> {
        @Option(description = "Target environment for the deployment run")
        String target;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "syn", description = "Synopsis", generateHelp = true)
    public static class SynopsisCommand implements Command<CommandInvocation> {
        @Option(description = "First long option")
        String firstLongOption;
        @Option(description = "Second long option")
        String secondLongOption;
        @Option(description = "Third long option")
        String thirdLongOption;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "widechild", description = "A deliberately long child description that wraps onto several lines at narrow widths", generateHelp = true)
    public static class WideChild implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "widegrp", description = "Group", generateHelp = true, groupCommands = { WideChild.class })
    public static class WideGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
