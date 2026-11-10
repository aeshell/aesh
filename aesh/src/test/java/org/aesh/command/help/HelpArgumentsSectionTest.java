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

import org.aesh.command.ArgumentsSection;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.HelpFeature;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.junit.Test;

/**
 * Positional arguments print under their own {@code Arguments:} heading
 * first or last when {@code argumentsSection} is set ({#751}, {#756}),
 * instead of trailing the last option section.
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
    public void testLastRendersAfterOptions() throws Exception {
        String help = help(new LastCommand());

        int usage = help.indexOf("Usage:");
        int options = help.indexOf("Options:");
        int arguments = help.indexOf("Arguments:");
        int docs = help.indexOf("Documentation:");
        assertTrue(help, usage >= 0 && options > usage && arguments > options && docs > arguments);
        int artifact = help.indexOf("<artifact>", arguments);
        assertTrue("positional lives in the trailing section, got:\n" + help, artifact > arguments);
        assertTrue("no other <artifact> after the section, got:\n" + help,
                help.indexOf("<artifact>", artifact + 1) < 0);
    }

    @Test
    public void testSharedDescriptionColumn() throws Exception {
        String help = help(new WideCommand());
        String[] lines = help.split("\\R");

        int optionDesc = -1;
        int positionalDesc = -1;
        boolean sameLine = false;
        for (String line : lines) {
            if (line.contains("opt desc"))
                optionDesc = line.indexOf("opt desc");
            if (line.contains("pos desc"))
                positionalDesc = line.indexOf("pos desc");
            if (line.contains("<veryLongPositionalName>"))
                sameLine = line.contains("pos desc");
        }
        assertTrue(help, optionDesc > 0 && positionalDesc > 0);
        assertEquals("option and positional descriptions align", optionDesc, positionalDesc);
        assertTrue("long positional name shares the widened column instead of wrapping, got:\n" + help, sameLine);
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
        builder.setArgumentsSectionDefault(ArgumentsSection.FIRST);
        CommandLineParser<CommandInvocation> parser = builder.create(new InlineCommand()).getParser();
        parser.updateAnsiMode(false);

        assertTrue(parser.printHelp(), parser.printHelp().contains("Arguments:"));
    }

    @Test
    public void testFleetDefaultLast() throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        builder.setArgumentsSectionDefault(ArgumentsSection.LAST);
        CommandLineParser<CommandInvocation> parser = builder.create(new InlineCommand()).getParser();
        parser.updateAnsiMode(false);

        String help = parser.printHelp();
        int arguments = help.indexOf("Arguments:");
        int options = help.indexOf("Options:");
        assertTrue("fleet LAST renders Arguments: after Options:, got:\n" + help,
                arguments >= 0 && options >= 0 && arguments > options);
    }

    @Test
    public void testExplicitOffBeatsFleet() throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        builder.setHelpDefaults(EnumSet.allOf(HelpFeature.class));
        builder.setArgumentsSectionDefault(ArgumentsSection.FIRST);
        CommandLineParser<CommandInvocation> parser = builder.create(new ExplicitOffCommand()).getParser();
        parser.updateAnsiMode(false);

        assertFalse(parser.printHelp(), parser.printHelp().contains("Arguments:"));
    }

    @Test
    public void testLateFleetUpdateReResolves() throws Exception {
        AeshCommandRegistryBuilder<CommandInvocation> builder = AeshCommandRegistryBuilder.builder();
        builder.command(InlineCommand.class);
        CommandRegistry<CommandInvocation> registry = builder.create();
        CommandLineParser<CommandInvocation> parser = registry.getCommand("deploy-inline", "").getParser();
        assertFalse(parser.printHelp(), parser.printHelp().contains("Arguments:"));
        builder.argumentsSectionDefault(ArgumentsSection.FIRST);
        assertTrue("late argumentsSectionDefault applies to already-built commands",
                parser.printHelp().contains("Arguments:"));
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

    @CommandDefinition(name = "deploy", description = "Deploy app", generateHelp = true, argumentsSection = ArgumentsSection.FIRST)
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

    @CommandDefinition(name = "wide", description = "Wide positional", generateHelp = true, argumentsSection = ArgumentsSection.FIRST)
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

    @CommandDefinition(name = "last", description = "Trailing section", generateHelp = true, argumentsSection = ArgumentsSection.LAST, helpUrl = "https://example.com/deploy")
    public static class LastCommand implements Command<CommandInvocation> {
        @Option(description = "Target environment")
        String target;

        @Argument(description = "The artifact to deploy")
        String artifact;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "pos-only", description = "Only positionals", argumentsSection = ArgumentsSection.FIRST)
    public static class PositionalOnlyCommand implements Command<CommandInvocation> {
        @Argument(description = "The artifact to deploy")
        String artifact;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "off", description = "Explicit off", generateHelp = true, argumentsSection = ArgumentsSection.OFF)
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

    @CommandDefinition(name = "section-group", description = "Section group", generateHelp = true, argumentsSection = ArgumentsSection.FIRST, groupCommands = {
            SectionChildCommand.class })
    public static class SectionGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
