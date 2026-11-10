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
        // COLUMNS may or may not be set here; either way the floor holds
        // and no terminal means no throw.
        String sysprop = System.getProperty("aesh.help.width");
        if (sysprop == null)
            assertTrue(helpWidth() >= 40);
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

    private static int helpWidth() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new WideCommand()).getParser();
        return parser.getProcessedCommand().helpWidth();
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

    private static void clearWidth() {
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
}
