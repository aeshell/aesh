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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.junit.Test;

/**
 * The help wraps a description that is longer than the help is wide, at a space, and aligns the next lines with the
 * description column.
 */
public class HelpWrapTest {

    private static final String LONG = "How long to wait for the integration to answer, in millis or as a duration "
            + "such as 30s, before the command gives up and reports a timeout";

    @Test
    public void testLongOptionDescriptionIsWrapped() throws Exception {
        List<String> lines = lines(help(new WaitCommand()));

        int first = indexOf(lines, "--timeout");
        int column = lines.get(first).indexOf("How long");
        List<String> words = new ArrayList<>(Arrays.asList(lines.get(first).substring(column).split(" ")));
        int i = first + 1;
        while (i < lines.size() && lines.get(i).startsWith(spaces(column)) && !lines.get(i).trim().isEmpty()) {
            // a continuation line starts at the description column
            assertTrue(lines.get(i), lines.get(i).charAt(column) != ' ');
            words.addAll(Arrays.asList(lines.get(i).trim().split(" ")));
            i++;
        }
        assertTrue("wrapped over more than one line", i > first + 1);
        assertEquals(LONG, String.join(" ", words));
        for (String line : lines) {
            assertTrue(line, line.length() <= 80);
        }
    }

    @Test
    public void testShortDescriptionIsNotWrapped() throws Exception {
        List<String> lines = lines(help(new WaitCommand()));

        int name = indexOf(lines, "--name");
        assertTrue(lines.get(name), lines.get(name).endsWith("The name"));
        assertTrue(lines.get(name + 1), lines.get(name + 1).trim().startsWith("--timeout"));
    }

    @Test
    public void testSubCommandDescriptionIsWrapped() throws Exception {
        List<String> lines = lines(help(new ToolCommand()));

        int first = indexOf(lines, "wait");
        assertTrue(lines.get(first + 1), lines.get(first + 1).startsWith(spaces(lines.get(first).indexOf("How long"))));
        for (String line : lines) {
            assertTrue(line, line.length() <= 80);
        }
    }

    @Test
    public void testCommandWithoutDescription() throws Exception {
        ProcessedCommand<?, ?> cmd = ProcessedCommandBuilder.builder()
                .name("plain")
                .create();

        List<String> lines = lines(cmd.printHelp("plain", false));
        assertEquals("", lines.get(0));
        assertTrue(lines.get(1), lines.get(1).contains("plain"));
    }

    @Test
    public void testWrapText() {
        assertEquals(Arrays.asList("one two three four", "five six"),
                ProcessedOption.wrapText("one two three four five six", 20));
        assertEquals(Arrays.asList("one two three"), ProcessedOption.wrapText("one two three", 20));
        // a word longer than the width keeps a line of its own
        assertEquals(Arrays.asList("a", "abcdefghijklmnopqrstuvwxyz", "b"),
                ProcessedOption.wrapText("a abcdefghijklmnopqrstuvwxyz b", 20));
        // the lines of a multi-line text are wrapped one by one, and keep their leading spaces
        assertEquals(Arrays.asList("first line", "  one two three four", "  five"),
                ProcessedOption.wrapText("first line\n  one two three four five", 20));
        // too narrow to wrap
        assertEquals(Arrays.asList("one two three"), ProcessedOption.wrapText("one two three", 5));
    }

    // Wrap-assumption tests below pin the width: help now follows the live
    // terminal width, so an ambient COLUMNS or wide terminal would otherwise
    // change the expected 80-column layout.
    private static String help(Command<CommandInvocation> command) throws Exception {
        System.setProperty("aesh.help.width", "80");
        try {
            CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>().create(command)
                    .getParser();
            parser.updateAnsiMode(false);
            return parser.printHelp();
        } finally {
            System.clearProperty("aesh.help.width");
        }
    }

    private static List<String> lines(String text) {
        return Arrays.asList(text.split("\\R"));
    }

    private static int indexOf(List<String> lines, String text) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().startsWith(text)) {
                return i;
            }
        }
        throw new AssertionError("No line with " + text + " in " + lines);
    }

    private static String spaces(int n) {
        return String.format("%" + n + "s", "");
    }

    @CommandDefinition(name = "wait", description = LONG, generateHelp = true)
    public static class WaitCommand implements Command<CommandInvocation> {
        @Option(description = "The name")
        String name;

        @Option(description = LONG)
        String timeout;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "tool", description = "A tool", groupCommands = { WaitCommand.class }, generateHelp = true)
    public static class ToolCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
