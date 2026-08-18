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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.OptionGroup;
import org.aesh.command.option.OptionList;
import org.junit.Test;

/**
 * Tests for new attributes added to @OptionList and @OptionGroup (#582).
 *
 * @author Aesh team
 */
public class OptionListGroupAttributeTest {

    // ---- @OptionList acceptNameWithoutDashes ----

    @CommandDefinition(name = "list-bare", description = "Test OptionList with acceptNameWithoutDashes")
    public static class ListBareCmd implements Command<CommandInvocation> {
        @OptionList(name = "items", acceptNameWithoutDashes = true, valueSeparator = ',')
        private List<String> items;

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testOptionListAcceptNameWithoutDashes() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>()
                .create(new ListBareCmd()).getParser();

        // Parse with bare name (no dashes)
        clp.parse("list-bare items=a,b,c", CommandLineParser.Mode.STRICT);
        ProcessedOption opt = clp.getProcessedCommand().findLongOptionNoActivatorCheck("items");
        assertNotNull(opt);
        assertTrue(opt.acceptNameWithoutDashes());
        assertEquals(3, opt.getValues().size());
        assertEquals("a", opt.getValues().get(0));
        assertEquals("b", opt.getValues().get(1));
        assertEquals("c", opt.getValues().get(2));
    }

    @Test
    public void testOptionListAcceptNameWithoutDashesAlsoWorkWithDashes() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>()
                .create(new ListBareCmd()).getParser();

        // Parse with dashes (should still work)
        clp.parse("list-bare --items=x,y", CommandLineParser.Mode.STRICT);
        ProcessedOption opt = clp.getProcessedCommand().findLongOptionNoActivatorCheck("items");
        assertNotNull(opt);
        assertEquals(2, opt.getValues().size());
        assertEquals("x", opt.getValues().get(0));
        assertEquals("y", opt.getValues().get(1));
    }

    // ---- @OptionGroup acceptNameWithoutDashes ----

    @CommandDefinition(name = "group-bare", description = "Test OptionGroup with acceptNameWithoutDashes")
    public static class GroupBareCmd implements Command<CommandInvocation> {
        @OptionGroup(shortName = 'D', acceptNameWithoutDashes = true)
        private Map<String, String> properties;

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testOptionGroupAcceptNameWithoutDashes() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>()
                .create(new GroupBareCmd()).getParser();

        // Parse with short name (standard group pattern)
        clp.parse("group-bare -Dkey1=val1 -Dkey2=val2", CommandLineParser.Mode.STRICT);
        ProcessedOption opt = clp.getProcessedCommand().findOptionNoActivatorCheck("D");
        assertNotNull(opt);
        assertTrue(opt.acceptNameWithoutDashes());
        assertEquals("val1", opt.getProperties().get("key1"));
        assertEquals("val2", opt.getProperties().get("key2"));
    }

    // ---- @OptionList overrideRequired ----

    @CommandDefinition(name = "list-override", description = "Test OptionList with overrideRequired")
    public static class ListOverrideCmd implements Command<CommandInvocation> {
        @OptionList(name = "help-items", overrideRequired = true)
        private List<String> helpItems;

        @org.aesh.command.option.Option(name = "required-opt", required = true)
        private String requiredOpt;

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testOptionListOverrideRequired() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>()
                .create(new ListOverrideCmd()).getParser();

        // Setting --help-items should skip required check for --required-opt
        clp.parse("list-override --help-items=a,b", CommandLineParser.Mode.STRICT);
        ProcessedOption opt = clp.getProcessedCommand().findLongOptionNoActivatorCheck("help-items");
        assertNotNull(opt);
        assertTrue(opt.doOverrideRequired());
        assertEquals(0, clp.getProcessedCommand().parserExceptions().size());
    }

    // ---- @OptionGroup aliases ----

    @CommandDefinition(name = "group-alias", description = "Test OptionGroup with aliases")
    public static class GroupAliasCmd implements Command<CommandInvocation> {
        @OptionGroup(shortName = 'D', name = "define", aliases = { "def", "prop" })
        private Map<String, String> properties;

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testOptionGroupAliases() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>()
                .create(new GroupAliasCmd()).getParser();

        ProcessedOption opt = clp.getProcessedCommand().findLongOptionNoActivatorCheck("define");
        assertNotNull(opt);
        assertTrue(opt.getAliases().contains("def"));
        assertTrue(opt.getAliases().contains("prop"));

        // Parse with alias (OptionGroup uses property syntax: --alias=key=val)
        clp.parse("group-alias --def=key=val", CommandLineParser.Mode.STRICT);
        assertEquals(0, clp.getProcessedCommand().parserExceptions().size());
    }

    // ---- @OptionList inherited ----

    @CommandDefinition(name = "list-inherit", description = "Test OptionList with inherited")
    public static class ListInheritCmd implements Command<CommandInvocation> {
        @OptionList(name = "tags", inherited = true, valueSeparator = ',')
        private List<String> tags;

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testOptionListInherited() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>()
                .create(new ListInheritCmd()).getParser();

        ProcessedOption opt = clp.getProcessedCommand().findLongOptionNoActivatorCheck("tags");
        assertNotNull(opt);
        assertTrue(opt.isInherited());
    }

    // ---- @OptionGroup inherited ----

    @CommandDefinition(name = "group-inherit", description = "Test OptionGroup with inherited")
    public static class GroupInheritCmd implements Command<CommandInvocation> {
        @OptionGroup(shortName = 'D', inherited = true)
        private Map<String, String> properties;

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testOptionGroupInherited() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>()
                .create(new GroupInheritCmd()).getParser();

        ProcessedOption opt = clp.getProcessedCommand().findOptionNoActivatorCheck("D");
        assertNotNull(opt);
        assertTrue(opt.isInherited());
    }

    // ---- @OptionGroup exclusiveWith ----

    @CommandDefinition(name = "group-excl", description = "Test OptionGroup with exclusiveWith")
    public static class GroupExclusiveCmd implements Command<CommandInvocation> {
        @OptionGroup(shortName = 'D', name = "define", exclusiveWith = { "env" })
        private Map<String, String> properties;

        @org.aesh.command.option.Option(name = "env")
        private String env;

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testOptionGroupExclusiveWith() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>()
                .create(new GroupExclusiveCmd()).getParser();

        ProcessedOption opt = clp.getProcessedCommand().findLongOptionNoActivatorCheck("define");
        assertNotNull(opt);
        assertTrue(opt.getExclusiveWith().contains("env"));
    }
}
