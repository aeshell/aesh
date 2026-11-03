/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.aesh.command.help;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Arguments;
import org.junit.Test;

/**
 * In the usage line of the help, an optional positional argument is in brackets, and a required one is not.
 */
public class ArgumentSynopsisTest {

    @Test
    public void testOptionalArgument() throws Exception {
        assertEquals("Usage: get [-h] [<name>]", usage(new OptionalArgumentCommand()));
    }

    @Test
    public void testRequiredArgument() throws Exception {
        assertEquals("Usage: get [-h] <name>", usage(new RequiredArgumentCommand()));
    }

    @Test
    public void testOptionalArguments() throws Exception {
        assertEquals("Usage: get [-h] [<names>]", usage(new OptionalArgumentsCommand()));
    }

    @Test
    public void testRequiredArguments() throws Exception {
        assertEquals("Usage: get [-h] <names>", usage(new RequiredArgumentsCommand()));
    }

    private static String usage(Command<CommandInvocation> command) throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>().create(command)
                .getParser();
        parser.updateAnsiMode(false);
        for (String line : parser.printHelp().split("\\R")) {
            if (line.startsWith("Usage:")) {
                return line.trim();
            }
        }
        return null;
    }

    @CommandDefinition(name = "get", description = "", generateHelp = true)
    public static class OptionalArgumentCommand implements Command<CommandInvocation> {
        @Argument(description = "The name")
        String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "get", description = "", generateHelp = true)
    public static class RequiredArgumentCommand implements Command<CommandInvocation> {
        @Argument(description = "The name", required = true)
        String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "get", description = "", generateHelp = true)
    public static class OptionalArgumentsCommand implements Command<CommandInvocation> {
        @Arguments(description = "The names")
        List<String> names;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "get", description = "", generateHelp = true)
    public static class RequiredArgumentsCommand implements Command<CommandInvocation> {
        @Arguments(description = "The names", required = true)
        List<String> names;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
