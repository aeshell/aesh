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

import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.junit.Test;

/**
 * With ANSI off, the help has no ANSI codes: not for the options, and not for the positional arguments either.
 */
public class HelpAnsiModeTest {

    private static final String ESC = String.valueOf((char) 27);

    @Test
    public void testHelpWithoutAnsi() throws Exception {
        CommandLineParser<CommandInvocation> parser = parser(new CopyCommand());
        parser.updateAnsiMode(false);

        String help = parser.printHelp();
        assertFalse(help, help.contains(ESC));
        assertTrue(help, help.contains("<source>"));
        assertTrue(help, help.contains("<targets>"));
    }

    @Test
    public void testHelpWithoutAnsiForTheSingleArgument() throws Exception {
        CommandLineParser<CommandInvocation> parser = parser(new RemoveCommand());
        parser.updateAnsiMode(false);

        String help = parser.printHelp();
        assertFalse(help, help.contains(ESC));
        assertTrue(help, help.contains("<file>"));
    }

    @Test
    public void testAnsiOffAndOnAgain() throws Exception {
        CommandLineParser<CommandInvocation> parser = parser(new CopyCommand());
        parser.updateAnsiMode(false);
        parser.updateAnsiMode(true);

        String help = parser.printHelp();
        assertTrue(help, help.contains(ESC));
    }

    private static CommandLineParser<CommandInvocation> parser(Command<CommandInvocation> command) throws Exception {
        return new AeshCommandContainerBuilder<CommandInvocation>().create(command).getParser();
    }

    @CommandDefinition(name = "copy", description = "Copy files", generateHelp = true)
    public static class CopyCommand implements Command<CommandInvocation> {
        @Option(hasValue = false, description = "Overwrite files that exist")
        boolean force;

        @Argument(description = "The file to copy", required = true)
        String source;

        @Arguments(description = "Where to copy it")
        List<String> targets;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "rm", description = "Remove a file", generateHelp = true)
    public static class RemoveCommand implements Command<CommandInvocation> {
        @Argument(description = "The file to remove")
        String file;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
