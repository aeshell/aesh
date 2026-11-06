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

import java.util.EnumSet;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.HelpFeature;
import org.aesh.command.Toggle;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.junit.Test;

/**
 * Positional arguments print under their own {@code Arguments:} heading
 * right after the usage line when {@code separateArgumentsSection} is on
 * ({#751}), instead of trailing the last option section.
 */
public class HelpArgumentsSectionTest {

    @Test
    public void testOffByDefaultKeepsInlineBehavior() throws Exception {
        String help = help(new InlineCommand());

        assertFalse(help, help.contains("Arguments:"));
        int options = help.indexOf("Options:");
        assertTrue(help, options >= 0);
        // positional still trails the options (search past the synopsis)
        assertTrue(help, help.indexOf("<artifact>", options) > options);
    }

    @Test
    public void testSeparateSectionAfterUsage() throws Exception {
        String help = help(new DeployCommand());

        int usage = help.indexOf("Usage:");
        int arguments = help.indexOf("Arguments:");
        int options = help.indexOf("Options:");
        assertTrue(help, usage >= 0 && arguments > usage && options > arguments);
        // positional lives in its section, not after Options:
        // (search past the synopsis, which also names it)
        int artifact = help.indexOf("<artifact>", arguments);
        assertTrue(help, artifact > arguments && artifact < options);
        assertTrue("no trailing positional after Options", help.indexOf("<artifact>", options) < 0);
    }

    @Test
    public void testSharedDescriptionColumn() throws Exception {
        String help = help(new WideCommand());
        String[] lines = help.split("\\R");

        int optionDesc = -1;
        int positionalDesc = -1;
        for (String line : lines) {
            if (line.contains("opt desc"))
                optionDesc = line.indexOf("opt desc");
            if (line.contains("pos desc"))
                positionalDesc = line.indexOf("pos desc");
        }
        assertTrue(help, optionDesc > 0 && positionalDesc > 0);
        assertEquals("option and positional descriptions align", optionDesc, positionalDesc);
    }

    @Test
    public void testNoOptionsRendersArgumentsHeading() throws Exception {
        String help = help(new PositionalOnlyCommand());

        assertTrue(help, help.contains("Arguments:"));
        assertFalse(help, help.contains("Options:"));
    }

    @Test
    public void testFleetDefault() throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        builder.setHelpDefaults(EnumSet.of(HelpFeature.SEPARATE_ARGUMENTS_SECTION));
        CommandLineParser<CommandInvocation> parser = builder.create(new InlineCommand()).getParser();
        parser.updateAnsiMode(false);

        assertTrue(parser.printHelp(), parser.printHelp().contains("Arguments:"));
    }

    @Test
    public void testExplicitOffBeatsFleet() throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        builder.setHelpDefaults(EnumSet.allOf(HelpFeature.class));
        CommandLineParser<CommandInvocation> parser = builder.create(new ExplicitOffCommand()).getParser();
        parser.updateAnsiMode(false);

        assertFalse(parser.printHelp(), parser.printHelp().contains("Arguments:"));
    }

    @Test
    public void testChildInheritsSectionFromParent() throws Exception {
        CommandLineParser<CommandInvocation> group = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new SectionGroup()).getParser();
        CommandLineParser<CommandInvocation> child = null;
        for (CommandLineParser<CommandInvocation> c : group.getAllChildParsers()) {
            if (c.getProcessedCommand().name().equals("section-child"))
                child = c;
        }
        assertTrue("group should have a section-child", child != null);
        child.updateAnsiMode(false);

        assertTrue(child.printHelp(), child.printHelp().contains("Arguments:"));
    }

    private static String help(Command<CommandInvocation> command) throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>().create(command)
                .getParser();
        parser.updateAnsiMode(false);
        return parser.printHelp();
    }

    @CommandDefinition(name = "deploy-inline", description = "Deploy app", generateHelp = true)
    public static class InlineCommand implements Command<CommandInvocation> {
        @Option(description = "Target environment")
        String target;

        @Argument(description = "The artifact to deploy")
        String artifact;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "deploy", description = "Deploy app", generateHelp = true, separateArgumentsSection = Toggle.ON)
    public static class DeployCommand implements Command<CommandInvocation> {
        @Option(description = "Target environment")
        String target;

        @Argument(description = "The artifact to deploy")
        String artifact;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "wide", description = "Wide positional", generateHelp = true, separateArgumentsSection = Toggle.ON)
    public static class WideCommand implements Command<CommandInvocation> {
        @Option(description = "opt desc")
        String x;

        @Argument(description = "pos desc")
        String veryLongPositionalName;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "pos-only", description = "Only positionals", separateArgumentsSection = Toggle.ON)
    public static class PositionalOnlyCommand implements Command<CommandInvocation> {
        @Argument(description = "The artifact to deploy")
        String artifact;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "off", description = "Explicit off", generateHelp = true, separateArgumentsSection = Toggle.OFF)
    public static class ExplicitOffCommand implements Command<CommandInvocation> {
        @Option(description = "Target environment")
        String target;

        @Argument(description = "The artifact to deploy")
        String artifact;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "section-child", description = "Inheriting child", generateHelp = true)
    public static class SectionChildCommand implements Command<CommandInvocation> {
        @Option(description = "Target environment")
        String target;

        @Argument(description = "The artifact to deploy")
        String artifact;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "section-group", description = "Section group", generateHelp = true, separateArgumentsSection = Toggle.ON, groupCommands = {
            SectionChildCommand.class })
    public static class SectionGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
