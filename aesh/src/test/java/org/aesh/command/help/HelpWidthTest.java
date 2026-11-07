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
import org.junit.After;
import org.junit.Test;

/**
 * The help width defaults to {@link ProcessedCommand#HELP_WIDTH} but can be
 * overridden with the {@code aesh.help.width} system property (or the
 * {@code COLUMNS} environment variable), clamped to a sane minimum.
 */
public class HelpWidthTest {

    @After
    public void clearProperty() {
        System.clearProperty("aesh.help.width");
    }

    @Test
    public void testDefaultWidthWhenNoOverride() {
        System.clearProperty("aesh.help.width");
        assertEquals(ProcessedCommand.HELP_WIDTH, ProcessedCommand.resolveHelpWidth());
    }

    @Test
    public void testSystemPropertyOverride() {
        System.setProperty("aesh.help.width", "120");
        assertEquals(120, ProcessedCommand.resolveHelpWidth());
    }

    @Test
    public void testTinyOverrideIsClamped() {
        System.setProperty("aesh.help.width", "5");
        assertTrue("a tiny width is clamped up, got: " + ProcessedCommand.resolveHelpWidth(),
                ProcessedCommand.resolveHelpWidth() >= 40);
    }

    @Test
    public void testNonNumericOverrideIgnored() {
        System.setProperty("aesh.help.width", "wide");
        assertEquals(ProcessedCommand.HELP_WIDTH, ProcessedCommand.resolveHelpWidth());
    }

    @Test
    public void testTerminalWidthSupplierUsedWhenNoOverride() throws Exception {
        ProcessedCommand<?, ?> pc = processedCommand(new WideCommand());
        pc.setTerminalWidthSupplier(() -> 100);
        assertEquals(100, pc.helpWidth());
    }

    @Test
    public void testOverrideBeatsTerminalWidthSupplier() throws Exception {
        System.setProperty("aesh.help.width", "120");
        ProcessedCommand<?, ?> pc = processedCommand(new WideCommand());
        pc.setTerminalWidthSupplier(() -> 100);
        assertEquals(120, pc.helpWidth());
    }

    @Test
    public void testSupplierFailureFallsBackToDefault() throws Exception {
        ProcessedCommand<?, ?> pc = processedCommand(new WideCommand());
        pc.setTerminalWidthSupplier(() -> {
            throw new IllegalStateException("no terminal");
        });
        assertEquals(ProcessedCommand.HELP_WIDTH, pc.helpWidth());
    }

    @Test
    public void testSupplierNonPositiveFallsBackToDefault() throws Exception {
        ProcessedCommand<?, ?> pc = processedCommand(new WideCommand());
        pc.setTerminalWidthSupplier(() -> 0);
        assertEquals(ProcessedCommand.HELP_WIDTH, pc.helpWidth());
    }

    @Test
    public void testSupplierTinyWidthIsClamped() throws Exception {
        ProcessedCommand<?, ?> pc = processedCommand(new WideCommand());
        pc.setTerminalWidthSupplier(() -> 5);
        assertTrue("tiny terminal width clamped, got: " + pc.helpWidth(), pc.helpWidth() >= 40);
    }

    @Test
    public void testDetectTerminalWidthIsSafeWithoutTerminal() {
        // In a non-terminal test/CI environment this must not throw and must
        // yield -1 (no tty) or a positive width; never zero or an exception.
        int width = ProcessedCommand.detectTerminalWidth();
        assertTrue("detectTerminalWidth returned " + width, width == -1 || width > 0);
    }

    @Test
    public void testDescriptionWrapsAtConfiguredWidth() throws Exception {
        System.setProperty("aesh.help.width", "120");
        String wide = help(new WideCommand());
        System.setProperty("aesh.help.width", "40");
        String narrow = help(new WideCommand());

        // The same help wraps onto more lines when the width is smaller.
        assertTrue("narrow help should wrap onto more lines than wide help\nwide:\n" + wide
                + "\nnarrow:\n" + narrow,
                narrow.split("\\R").length > wide.split("\\R").length);
    }

    private static String help(Command<CommandInvocation> command) throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(command).getParser();
        parser.updateAnsiMode(false);
        return parser.printHelp();
    }

    private static ProcessedCommand<?, ?> processedCommand(Command<CommandInvocation> command) throws Exception {
        return new AeshCommandContainerBuilder<CommandInvocation>().create(command).getParser().getProcessedCommand();
    }

    @CommandDefinition(name = "wide", description = "This command has a deliberately long description so that it wraps differently depending on the configured help width used by the renderer.", generateHelp = true)
    public static class WideCommand implements Command<CommandInvocation> {
        @Option(description = "A fairly long option description that also wraps when the help width is made narrow enough to force it")
        String value;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
