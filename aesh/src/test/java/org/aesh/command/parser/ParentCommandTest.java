/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command.parser;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.impl.activator.AeshCommandActivatorProvider;
import org.aesh.command.impl.activator.AeshOptionActivatorProvider;
import org.aesh.command.impl.completer.AeshCompleterInvocationProvider;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.context.CommandContext;
import org.aesh.command.impl.converter.AeshConverterInvocationProvider;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.validator.AeshValidatorInvocationProvider;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.option.ParentCommand;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.AeshContext;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for @ParentCommand annotation and CommandContext.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParentCommandTest {

    private final InvocationProviders invocationProviders = new AeshInvocationProviders(
            SettingsBuilder.builder()
                    .converterInvocationProvider(new AeshConverterInvocationProvider())
                    .completerInvocationProvider(new AeshCompleterInvocationProvider<>())
                    .validatorInvocationProvider(new AeshValidatorInvocationProvider())
                    .optionActivatorProvider(new AeshOptionActivatorProvider())
                    .commandActivatorProvider(new AeshCommandActivatorProvider()).build());

    @Test
    public void testCommandContext() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new ModuleGroupCommand<>()).getParser();

        // Parse the group command with options
        parser.parse("module --verbose --name=my-module-name", CommandLineParser.Mode.STRICT);
        parser.getCommandPopulator().populateObject(
                parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        ModuleGroupCommand<CommandInvocation> moduleCmd =
                (ModuleGroupCommand<CommandInvocation>) parser.getCommand();

        // Create a CommandContext and push the parent
        CommandContext ctx = new CommandContext("aesh> ");
        ctx.push(parser, moduleCmd);

        // Verify context state
        assertTrue(ctx.isInSubCommandMode());
        assertEquals("module", ctx.getContextPath());
        assertEquals("my-module-name", ctx.getParentValue("moduleName", String.class));
        assertTrue(ctx.getParentValue("verbose", Boolean.class));

        // Verify prompt building (shows option value "name" in prompt)
        assertEquals("module[my-module-name]> ", ctx.buildPrompt(true));
        assertEquals("module> ", ctx.buildPrompt(false));
    }

    @Test
    public void testParentCommandInjection() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        // Step 1: Parse and populate the parent command (simulating entering sub-command mode)
        CommandLineParser<CommandInvocation> parentParser = new AeshCommandContainerBuilder<>()
                .create(new ModuleGroupCommand<>()).getParser();
        parentParser.parse("module --verbose --name=my-module-name", CommandLineParser.Mode.STRICT);
        parentParser.getCommandPopulator().populateObject(
                parentParser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        ModuleGroupCommand<CommandInvocation> moduleCmd =
                (ModuleGroupCommand<CommandInvocation>) parentParser.getCommand();

        // Verify parent values
        assertEquals("my-module-name", moduleCmd.moduleName);
        assertTrue(moduleCmd.verbose);

        // Step 2: Create context with parent
        CommandContext ctx = new CommandContext("aesh> ");
        ctx.push(parentParser, moduleCmd);

        // Step 3: Get the child parser and parse subcommand (simulating typing in sub-command mode)
        CommandLineParser<CommandInvocation> childParser = parentParser.getChildParser("tag");
        assertNotNull(childParser);
        childParser.parse("tag v1.0", CommandLineParser.Mode.STRICT);

        // Step 4: Populate the subcommand with context to inject parent
        childParser.getCommandPopulator().populateObject(
                childParser.getProcessedCommand(), invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE, ctx);

        TagCommand<CommandInvocation> tagCmd =
                (TagCommand<CommandInvocation>) childParser.getCommand();

        // Verify subcommand's own argument was populated
        assertEquals("v1.0", tagCmd.tagName);

        // Verify parent was injected
        assertNotNull(tagCmd.parent);
        assertEquals("my-module-name", tagCmd.parent.moduleName);
        assertTrue(tagCmd.parent.verbose);
    }

    @Test
    public void testNestedContext() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        // Create a nested context
        CommandContext ctx = new CommandContext("aesh> ");

        // Simulate entering first level
        CommandLineParser<CommandInvocation> parser1 = new AeshCommandContainerBuilder<>()
                .create(new ModuleGroupCommand<>()).getParser();
        parser1.parse("module --verbose --name=my-module", CommandLineParser.Mode.STRICT);
        parser1.getCommandPopulator().populateObject(
                parser1.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        ctx.push(parser1, parser1.getCommand());

        assertEquals(1, ctx.depth());
        assertEquals("module", ctx.getContextPath());

        // Simulate entering second level (nested group)
        CommandLineParser<CommandInvocation> parser2 = new AeshCommandContainerBuilder<>()
                .create(new ProjectGroupCommand<>()).getParser();
        parser2.parse("project --name my-project", CommandLineParser.Mode.STRICT);
        parser2.getCommandPopulator().populateObject(
                parser2.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        ctx.push(parser2, parser2.getCommand());

        assertEquals(2, ctx.depth());
        assertEquals("module:project", ctx.getContextPath());
        assertEquals("module project", ctx.getContextPathWithSpaces());

        // Access values from both levels
        assertEquals("my-module", ctx.getParentValue("moduleName", String.class));
        assertEquals("my-project", ctx.getParentValue("name", String.class));
        assertTrue(ctx.getParentValue("verbose", Boolean.class, false));

        // Pop one level
        ctx.pop();
        assertEquals(1, ctx.depth());
        assertEquals("module", ctx.getContextPath());

        // Pop to exit
        ctx.pop();
        assertFalse(ctx.isInSubCommandMode());
        assertEquals("aesh> ", ctx.buildPrompt(true));
    }

    @Test
    public void testFormatContextValues() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new ModuleGroupCommand<>()).getParser();

        parser.parse("module --verbose --name=my-module-name", CommandLineParser.Mode.STRICT);
        parser.getCommandPopulator().populateObject(
                parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        CommandContext ctx = new CommandContext("aesh> ");
        ctx.push(parser, parser.getCommand());

        String formatted = ctx.formatContextValues();
        assertTrue(formatted.contains("module"));
        assertTrue(formatted.contains("name") || formatted.contains("my-module-name"));
        assertTrue(formatted.contains("verbose"));
    }

    @Test
    public void testInheritedOptions() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        // Step 1: Parse and populate the parent command with inherited options
        CommandLineParser<CommandInvocation> parentParser = new AeshCommandContainerBuilder<>()
                .create(new InheritedGroupCommand<>()).getParser();
        parentParser.parse("inherited --debug --level=5", CommandLineParser.Mode.STRICT);
        parentParser.getCommandPopulator().populateObject(
                parentParser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        InheritedGroupCommand<CommandInvocation> parentCmd =
                (InheritedGroupCommand<CommandInvocation>) parentParser.getCommand();

        // Verify parent values
        assertTrue(parentCmd.debug);
        assertEquals(Integer.valueOf(5), parentCmd.level);

        // Step 2: Create context with parent
        CommandContext ctx = new CommandContext("aesh> ");
        ctx.push(parentParser, parentCmd);

        // Step 3: Verify inherited values are available
        assertTrue(ctx.getInheritedValue("debug", Boolean.class));
        assertEquals(Integer.valueOf(5), ctx.getInheritedValue("level", Integer.class));

        // Step 4: Parse and populate a subcommand
        CommandLineParser<CommandInvocation> childParser = parentParser.getChildParser("sub");
        assertNotNull(childParser);
        childParser.parse("sub --extra=extra-value", CommandLineParser.Mode.STRICT);

        // Populate with context - inherited values should be auto-populated
        childParser.getCommandPopulator().populateObject(
                childParser.getProcessedCommand(), invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE, ctx);

        InheritedSubCommand<CommandInvocation> subCmd =
                (InheritedSubCommand<CommandInvocation>) childParser.getCommand();

        // Verify subcommand's own option was populated
        assertEquals("extra-value", subCmd.extra);

        // Verify inherited options were auto-populated (same field names)
        assertTrue(subCmd.debug);
        assertEquals(Integer.valueOf(5), subCmd.level);
    }

    @Test
    public void testInheritedOptionsNotOverridden() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        // Parse and populate the parent with inherited options
        CommandLineParser<CommandInvocation> parentParser = new AeshCommandContainerBuilder<>()
                .create(new InheritedGroupCommand<>()).getParser();
        parentParser.parse("inherited --debug --level=5", CommandLineParser.Mode.STRICT);
        parentParser.getCommandPopulator().populateObject(
                parentParser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        CommandContext ctx = new CommandContext("aesh> ");
        ctx.push(parentParser, parentParser.getCommand());

        // Parse subcommand with explicit value for an inherited option
        CommandLineParser<CommandInvocation> childParser = parentParser.getChildParser("sub");
        childParser.parse("sub --level=10 --extra=test", CommandLineParser.Mode.STRICT);

        childParser.getCommandPopulator().populateObject(
                childParser.getProcessedCommand(), invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE, ctx);

        InheritedSubCommand<CommandInvocation> subCmd =
                (InheritedSubCommand<CommandInvocation>) childParser.getCommand();

        // User-specified value should NOT be overridden by inherited value
        assertEquals(Integer.valueOf(10), subCmd.level);

        // debug should still be inherited (not explicitly set by user)
        assertTrue(subCmd.debug);
    }

    // ========== Test Command Classes ==========

    @GroupCommandDefinition(name = "module", description = "Module management",
            groupCommands = {TagCommand.class})
    public static class ModuleGroupCommand<CI extends CommandInvocation> implements Command<CI> {

        @Option(name = "verbose", shortName = 'v', hasValue = false, description = "Verbose mode")
        public boolean verbose;

        @Option(name = "name", shortName = 'n', description = "Module name")
        public String moduleName;

        public String getModuleName() {
            return moduleName;
        }

        public boolean isVerbose() {
            return verbose;
        }

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "tag", description = "Manage tags")
    public static class TagCommand<CI extends CommandInvocation> implements Command<CI> {

        @ParentCommand
        public ModuleGroupCommand<CI> parent;

        @Argument(description = "Tag name")
        public String tagName;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "project", description = "Project management",
            groupCommands = {})
    public static class ProjectGroupCommand<CI extends CommandInvocation> implements Command<CI> {

        @Option(name = "name", shortName = 'n', description = "Project name")
        public String name;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    // ========== Inherited Options Test Commands ==========

    @GroupCommandDefinition(name = "inherited", description = "Test inherited options",
            groupCommands = {InheritedSubCommand.class})
    public static class InheritedGroupCommand<CI extends CommandInvocation> implements Command<CI> {

        @Option(name = "debug", shortName = 'd', hasValue = false, description = "Debug mode", inherited = true)
        public boolean debug;

        @Option(name = "level", shortName = 'l', description = "Log level", inherited = true)
        public Integer level;

        @Option(name = "config", shortName = 'c', description = "Config file (not inherited)")
        public String config;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "sub", description = "Subcommand with inherited options")
    public static class InheritedSubCommand<CI extends CommandInvocation> implements Command<CI> {

        // These options match the parent's inherited options and will be auto-populated
        @Option(name = "debug", shortName = 'd', hasValue = false, description = "Debug mode")
        public boolean debug;

        @Option(name = "level", shortName = 'l', description = "Log level")
        public Integer level;

        // This is a subcommand-specific option
        @Option(name = "extra", shortName = 'e', description = "Extra option")
        public String extra;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }
}
