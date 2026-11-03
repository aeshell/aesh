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
import org.aesh.command.MethodCommand;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.junit.Test;

/**
 * The {@code argument} of an {@code @Option} is the label of its value in the help.
 */
public class OptionArgumentLabelTest {

    @Test
    public void testArgumentOfAField() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new LogCommand()).getParser();

        assertEquals("LEVEL", parser.getProcessedCommand().findLongOption("level").getArgument());
        parser.updateAnsiMode(false);
        String help = parser.printHelp();
        assertTrue(help, help.contains("--level=<LEVEL>"));
        // without an argument, the label is the name of the option
        assertTrue(help, help.contains("--name=<name>"));
    }

    @Test
    public void testArgumentOfAMethodParameter() throws Exception {
        LogMethods methods = new LogMethods();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(methods, LogMethods.class.getMethod("log", String.class)).getParser();

        ProcessedCommand<?, ?> command = parser.getProcessedCommand();
        assertEquals("LEVEL", command.findLongOption("level").getArgument());
        parser.updateAnsiMode(false);
        String help = parser.printHelp();
        assertTrue(help, help.contains("--level=<LEVEL>"));
    }

    @CommandDefinition(name = "log", description = "Change the logging", generateHelp = true)
    public static class LogCommand implements Command<CommandInvocation> {
        @Option(name = "level", argument = "LEVEL", description = "The logging level")
        String level;

        @Option(name = "name", description = "The name of the logger")
        String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class LogMethods {
        @MethodCommand(name = "log", description = "Change the logging")
        public void log(@Option(name = "level", argument = "LEVEL", description = "The logging level") String level) {
        }
    }
}
