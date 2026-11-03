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

import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.junit.Test;

/**
 * With {@code showDefaultValues}, the help shows the default value of each option and positional argument that has
 * one, on a line under its description.
 */
public class HelpDefaultValuesTest {

    @Test
    public void testDefaultValuesUnderTheDescription() throws Exception {
        String help = help(new ListCommand());
        String[] lines = help.split("\\R");

        int sort = lineOf(lines, "--sort");
        assertEquals(help, "Default: name", lines[sort + 1].trim());
        // aligned with the description of the option
        assertEquals(help, lines[sort].indexOf("Sort by"), lines[sort + 1].indexOf("Default:"));

        assertEquals(help, "Default: a, b", lines[lineOf(lines, "--tags") + 1].trim());
        assertEquals(help, "Default: false", lines[lineOf(lines, "--json") + 1].trim());
        assertEquals(help, "Default: *", lines[lineOf(lines, "<name>") + 1].trim());
    }

    @Test
    public void testNoLineWithoutDefaultValue() throws Exception {
        String[] lines = help(new ListCommand()).split("\\R");

        int filter = lineOf(lines, "--filter");
        assertFalse(lines[filter + 1].contains("Default:"));
    }

    @Test
    public void testNoDefaultValuesWhenNotAsked() throws Exception {
        String help = help(new PlainListCommand());

        assertTrue(help, help.contains("--sort"));
        assertFalse(help, help.contains("Default:"));
    }

    private static String help(Command<CommandInvocation> command) throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>().create(command)
                .getParser();
        parser.updateAnsiMode(false);
        return parser.printHelp();
    }

    // the line of the option or positional argument in the list under "Options:" (not in the usage line)
    private static int lineOf(String[] lines, String text) {
        boolean options = false;
        for (int i = 0; i < lines.length; i++) {
            options |= lines[i].equals("Options:");
            if (options && (lines[i].contains(" " + text + " ") || lines[i].contains(" " + text + "=")
                    || lines[i].contains(text))) {
                return i;
            }
        }
        throw new AssertionError("No line with " + text);
    }

    @CommandDefinition(name = "list", description = "List things", generateHelp = true, showDefaultValues = true)
    public static class ListCommand implements Command<CommandInvocation> {
        @Option(description = "Sort by name or age", defaultValue = "name")
        String sort;

        @Option(description = "Only the things that match")
        String filter;

        @OptionList(description = "The tags", defaultValue = { "a", "b" })
        List<String> tags;

        @Option(hasValue = false, description = "Output as JSON", defaultValue = "false")
        boolean json;

        @Argument(description = "The name of the thing", defaultValue = "*")
        String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "list", description = "List things", generateHelp = true)
    public static class PlainListCommand implements Command<CommandInvocation> {
        @Option(description = "Sort by name or age", defaultValue = "name")
        String sort;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
