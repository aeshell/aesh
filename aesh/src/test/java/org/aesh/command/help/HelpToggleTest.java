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

import java.util.EnumSet;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.HelpFeature;
import org.aesh.command.Toggle;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.junit.Test;

/**
 * Fleet-wide help toggles ({#748}): {@code Toggle} resolution order —
 * explicit wins, then parent, then fleet default, else off — plus
 * parent-to-child inheritance (eager and lazy) and late fleet updates.
 */
public class HelpToggleTest {

    @Test
    public void testInheritDefaultsToOff() throws Exception {
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> pc = build(null, null, null, null, null);
        assertFalse(pc.collapseSynopsis());
        assertFalse(pc.defaultOptionsFirst());
        assertFalse(pc.showDefaultValues());
        assertFalse(pc.failOnDuplicateOptions());
    }

    @Test
    public void testExplicitOnOffWithoutParentOrFleet() throws Exception {
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> pc = build(Toggle.ON, Toggle.OFF, Toggle.ON, Toggle.OFF,
                null);
        assertTrue(pc.collapseSynopsis());
        assertFalse(pc.defaultOptionsFirst());
        assertTrue(pc.showDefaultValues());
        assertFalse(pc.failOnDuplicateOptions());
    }

    @Test
    public void testFleetDefaultAppliesToInherit() throws Exception {
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> pc = build(null, null, null, null,
                EnumSet.of(HelpFeature.COLLAPSE_SYNOPSIS));
        assertTrue(pc.collapseSynopsis());
        assertFalse(pc.defaultOptionsFirst());
    }

    @Test
    public void testExplicitWinsOverParentAndFleet() throws Exception {
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> parent = build(Toggle.ON, Toggle.ON, Toggle.ON,
                Toggle.ON, null);
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> child = build(Toggle.OFF, Toggle.OFF, Toggle.OFF,
                Toggle.OFF, null);
        child.resolveHelpToggles(parent, EnumSet.allOf(HelpFeature.class), null);
        assertFalse(child.collapseSynopsis());
        assertFalse(child.defaultOptionsFirst());
        assertFalse(child.showDefaultValues());
        assertFalse(child.failOnDuplicateOptions());

        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> on = build(Toggle.ON, Toggle.ON, Toggle.ON, Toggle.ON,
                null);
        on.resolveHelpToggles(null, null, null);
        assertTrue(on.collapseSynopsis());
    }

    @Test
    public void testParentWinsOverFleet() throws Exception {
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> parentOff = build(Toggle.OFF, null, null, null,
                EnumSet.of(HelpFeature.COLLAPSE_SYNOPSIS));
        assertFalse(parentOff.collapseSynopsis());
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> child = build(null, null, null, null, null);
        child.resolveHelpToggles(parentOff, EnumSet.of(HelpFeature.COLLAPSE_SYNOPSIS), null);
        assertFalse("parent OFF beats fleet ON", child.collapseSynopsis());

        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> parentOn = build(Toggle.ON, null, null, null, null);
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> child2 = build(null, null, null, null, null);
        child2.resolveHelpToggles(parentOn, null, null);
        assertTrue("INHERIT takes parent value with no fleet", child2.collapseSynopsis());
    }

    @Test
    public void testNullFleetMeansOff() throws Exception {
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> pc = build(Toggle.ON, null, null, null, null);
        pc.resolveHelpToggles(null, null, null);
        assertTrue(pc.collapseSynopsis());
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> inherit = build(null, null, null, null, null);
        inherit.resolveHelpToggles(null, null, null);
        assertFalse(inherit.collapseSynopsis());
    }

    @Test
    public void testChildInheritsParentOnAttach() throws Exception {
        CommandContainer<CommandInvocation> group = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new ParentOnGroup());
        CommandLineParser<CommandInvocation> child = findChild(group.getParser(), "child");
        assertTrue("INHERIT child picks up parent ON", child.getProcessedCommand().collapseSynopsis());
        assertTrue("INHERIT child picks up parent ON", child.getProcessedCommand().showDefaultValues());
    }

    @Test
    public void testExplicitChildOffSurvivesAttach() throws Exception {
        CommandContainer<CommandInvocation> group = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new ParentOnGroup());
        CommandLineParser<CommandInvocation> child = findChild(group.getParser(), "strict");
        assertTrue("explicit ON survives attach", child.getProcessedCommand().failOnDuplicateOptions());
        assertFalse("explicit OFF survives parent ON", child.getProcessedCommand().collapseSynopsis());
    }

    @Test
    public void testLazyChildInheritsOnResolve() throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        builder.setLazyChildResolution(true);
        CommandContainer<CommandInvocation> group = builder.create(new ParentOnGroup());
        // Force lazy resolution through the attach hook
        group.getParser().getAllChildParsers();
        CommandLineParser<CommandInvocation> child = findChild(group.getParser(), "child");
        assertTrue("lazy INHERIT child picks up parent ON", child.getProcessedCommand().collapseSynopsis());
    }

    @Test
    public void testFleetDefaultsViaBuilder() throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        builder.setHelpDefaults(EnumSet.of(HelpFeature.COLLAPSE_SYNOPSIS, HelpFeature.SHOW_DEFAULT_VALUES));
        CommandContainer<CommandInvocation> container = builder.create(new PlainCommand());
        assertTrue(container.getParser().getProcessedCommand().collapseSynopsis());
        assertTrue(container.getParser().getProcessedCommand().showDefaultValues());
        assertFalse(container.getParser().getProcessedCommand().defaultOptionsFirst());
    }

    @Test
    public void testExplicitOffBeatsFleet() throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        builder.setHelpDefaults(EnumSet.allOf(HelpFeature.class));
        CommandContainer<CommandInvocation> container = builder.create(new ExplicitOffCommand());
        assertFalse(container.getParser().getProcessedCommand().collapseSynopsis());
        assertTrue(container.getParser().getProcessedCommand().failOnDuplicateOptions());
    }

    @Test
    public void testLateFleetUpdateReResolves() throws Exception {
        AeshCommandRegistryBuilder<CommandInvocation> builder = AeshCommandRegistryBuilder.builder();
        builder.command(PlainCommand.class);
        CommandRegistry<CommandInvocation> registry = builder.create();
        CommandLineParser<CommandInvocation> parser = registry.getCommand("plain", "").getParser();
        assertFalse(parser.getProcessedCommand().collapseSynopsis());
        builder.helpDefaults(EnumSet.of(HelpFeature.COLLAPSE_SYNOPSIS));
        assertTrue("late helpDefaults applies to already-built commands",
                parser.getProcessedCommand().collapseSynopsis());
    }

    @Test
    public void testFleetCollapseRendersCollapsedSynopsis() throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        builder.setHelpDefaults(EnumSet.of(HelpFeature.COLLAPSE_SYNOPSIS));
        CommandContainer<CommandInvocation> container = builder.create(new PlainCommand());
        container.getParser().updateAnsiMode(false);
        String help = container.getParser().printHelp();
        assertTrue("fleet COLLAPSE_SYNOPSIS collapses Usage, got:\n" + help, help.contains("[OPTIONS]"));
    }

    private static ProcessedCommand<Command<CommandInvocation>, CommandInvocation> build(Toggle collapse,
            Toggle first, Toggle show, Toggle strict, EnumSet<HelpFeature> globals) throws Exception {
        return ProcessedCommandBuilder.<Command<CommandInvocation>, CommandInvocation> builder()
                .name("x").description("y")
                .collapseSynopsis(collapse == null ? Toggle.INHERIT : collapse)
                .defaultOptionsFirst(first == null ? Toggle.INHERIT : first)
                .showDefaultValues(show == null ? Toggle.INHERIT : show)
                .failOnDuplicateOptions(strict == null ? Toggle.INHERIT : strict)
                .helpDefaults(globals)
                .create();
    }

    private static CommandLineParser<CommandInvocation> findChild(CommandLineParser<CommandInvocation> parent,
            String name) {
        for (CommandLineParser<CommandInvocation> child : parent.getAllChildParsers()) {
            if (child.getProcessedCommand().name().equals(name))
                return child;
        }
        throw new AssertionError("No child named " + name);
    }

    @CommandDefinition(name = "plain", description = "Plain")
    public static class PlainCommand implements Command<CommandInvocation> {
        @Option(description = "Config")
        String config;

        @Option(description = "Verbose", hasValue = false)
        boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "off", description = "Explicit off", collapseSynopsis = Toggle.OFF)
    public static class ExplicitOffCommand implements Command<CommandInvocation> {
        @Option(description = "A value")
        String value;

        @Option(description = "Other")
        String other;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child", description = "Inheriting child")
    public static class InheritChild implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "strict", description = "Strict child", failOnDuplicateOptions = Toggle.ON, collapseSynopsis = Toggle.OFF)
    public static class ExplicitChild implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "parent", description = "Parent group", collapseSynopsis = Toggle.ON, showDefaultValues = Toggle.ON, groupCommands = {
            InheritChild.class, ExplicitChild.class })
    public static class ParentOnGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
