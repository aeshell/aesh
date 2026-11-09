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

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.Toggle;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.junit.Test;

/**
 * A positional with a {@code helpGroup} renders inside that option group
 * instead of the positional flow, sharing the group's order namespace
 * ({#755}).
 */
public class HelpGroupedPositionalsTest {

    @Test
    public void testGroupedPositionalRendersInGroup() throws Exception {
        String help = help(new DeployCommand());

        int group = help.indexOf("Environments:");
        assertTrue(help, group >= 0);
        int artifact = help.indexOf("<artifact>", group);
        assertTrue("grouped positional renders in its group, got:\n" + help, artifact > group);
        assertFalse("no Arguments: section for fully grouped positionals", help.contains("Arguments:"));
        int options = help.indexOf("Options:");
        assertTrue("no trailing positional after Options", help.indexOf("<artifact>", options) < 0);
    }

    @Test
    public void testOrderInterleavesWithOptions() throws Exception {
        String help = help(new DeployCommand());

        int group = help.indexOf("Environments:");
        int target = help.indexOf("--target", group);
        int artifact = help.indexOf("<artifact>", group);
        int force = help.indexOf("--force", group);
        assertTrue("order 1, 2, 3 renders target, artifact, force, got:\n" + help,
                target > group && artifact > target && force > artifact);
    }

    @Test
    public void testOptionsBeforePositionalsOnTie() throws Exception {
        String help = help(new TiedCommand());

        int group = help.indexOf("Environments:");
        int options = help.indexOf("Options:");
        int opt = help.indexOf("--target", group);
        int artifact = help.indexOf("<artifact>", group);
        assertTrue("unordered option prints before unordered positional, got:\n" + help,
                opt > group && artifact > opt && artifact < options);
    }

    @Test
    public void testMixedGroupedAndPlain() throws Exception {
        String help = help(new MixedCommand());

        int arguments = help.indexOf("Arguments:");
        int group = help.indexOf("Environments:");
        assertTrue(help, arguments >= 0 && group >= 0 && arguments < group);
        int plain = help.indexOf("<plain>", arguments);
        assertTrue("ungrouped positional stays in Arguments:, got:\n" + help, plain > arguments && plain < group);
        assertTrue("grouped positional moves to its group, got:\n" + help,
                help.indexOf("<grouped>", arguments) > group);
    }

    @Test
    public void testOrderAppliesInArgumentsSection() throws Exception {
        String help = help(new OrderedPlainCommand());

        int arguments = help.indexOf("Arguments:");
        int second = help.indexOf("<second>", arguments);
        int first = help.indexOf("<first>", arguments);
        assertTrue("lower order prints first even against index order, got:\n" + help,
                second > arguments && first > second);
    }

    @Test
    public void testGroupedOutOfInlineTailByDefault() throws Exception {
        String help = help(new DeployCommand());
        // no separateArgumentsSection: grouped positional still leaves the inline tail
        assertFalse(help, help.contains("Arguments:"));
        int options = help.indexOf("Options:");
        assertTrue("no trailing positional after Options", help.indexOf("<artifact>", options) < 0);
    }

    private static String help(Command<CommandInvocation> command) throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>().create(command)
                .getParser();
        parser.updateAnsiMode(false);
        return parser.printHelp();
    }

    @CommandDefinition(name = "deploy", description = "Deploy app", generateHelp = true)
    public static class DeployCommand implements Command<CommandInvocation> {
        @Option(description = "Target environment", helpGroup = "Environments", order = 1)
        String target;

        @Option(description = "Force deploy", helpGroup = "Environments", order = 3, hasValue = false)
        boolean force;

        @Option(description = "Verbose output")
        boolean verbose;

        @Argument(description = "The artifact to deploy", helpGroup = "Environments", order = 2)
        String artifact;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "tied", description = "Tie order", generateHelp = true)
    public static class TiedCommand implements Command<CommandInvocation> {
        @Option(description = "Target environment", helpGroup = "Environments")
        String target;

        @Argument(description = "The artifact to deploy", helpGroup = "Environments")
        String artifact;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mixed", description = "Mixed positionals", generateHelp = true, separateArgumentsSection = Toggle.ON)
    public static class MixedCommand implements Command<CommandInvocation> {
        @Option(description = "Target environment", helpGroup = "Environments")
        String target;

        @Argument(description = "Plain artifact", index = "0")
        String plain;

        @Argument(description = "Grouped artifact", index = "1", helpGroup = "Environments")
        String grouped;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "ordered", description = "Ordered plain positionals", generateHelp = true, separateArgumentsSection = Toggle.ON)
    public static class OrderedPlainCommand implements Command<CommandInvocation> {
        @Argument(description = "First filled", index = "0", order = 2)
        String first;

        @Argument(description = "Second filled", index = "1", order = 1)
        String second;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
