/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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
package org.aesh.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.aesh.command.registry.CommandRegistry;
import org.junit.Before;
import org.junit.Test;

/**
 * {@code CommandInvocation} reports which options were explicitly given on
 * the command line, as opposed to set from defaults, fallbacks, a
 * {@code DefaultValueProvider}, prompts or selectors (#707).
 */
public class MatchedOptionTest {

    private static boolean dirMatched;
    private static List<String> dirValues;
    private static boolean levelMatched;
    private static List<String> levelValues;
    private static boolean portMatched;
    private static List<String> portValues;
    private static boolean verboseMatched;
    private static List<String> verboseValues;
    private static boolean findMatched;
    private static List<String> findValues;
    private static boolean dynMatched;
    private static Set<String> matchedNames;

    @Before
    public void reset() {
        dirMatched = false;
        dirValues = null;
        levelMatched = false;
        levelValues = null;
        portMatched = false;
        portValues = null;
        verboseMatched = false;
        verboseValues = null;
        findMatched = false;
        findValues = null;
        dynMatched = false;
        matchedNames = null;
    }

    private static void capture(CommandInvocation inv) {
        dirMatched = inv.hasMatchedOption("dir");
        dirValues = inv.matchedOptionValues("dir");
        levelMatched = inv.hasMatchedOption("--level");
        levelValues = inv.matchedOptionValues("level");
        portMatched = inv.hasMatchedOption("port");
        portValues = inv.matchedOptionValues("port");
        verboseMatched = inv.hasMatchedOption("v");
        verboseValues = inv.matchedOptionValues("--verbose");
        findMatched = inv.hasMatchedOption("find");
        findValues = inv.matchedOptionValues("find");
        dynMatched = inv.hasMatchedOption("dyn");
        matchedNames = inv.matchedOptionNames();
    }

    public static class MatchDvp implements DefaultValueProvider {
        @Override
        public String defaultValue(ProcessedOption option) {
            return "dyn".equals(option.name()) ? "fromprovider" : null;
        }

        @Override
        public String fallbackValue(ProcessedOption option) {
            return null;
        }
    }

    @CommandDefinition(name = "match", description = "match", defaultValueProvider = MatchDvp.class)
    public static class MatchCommand implements Command<CommandInvocation> {
        @Option(name = "dir", description = "dir")
        String dir;

        @Option(name = "level", description = "level", defaultValue = "INFO")
        String level;

        @Option(name = "port", description = "port", defaultValue = "8080")
        int port;

        @Option(name = "verbose", shortName = 'v', hasValue = false, description = "verbose")
        boolean verbose;

        @OptionList(name = "find", optionalValue = true, description = "find")
        List<String> find;

        @Option(name = "dyn", description = "dynamic")
        String dyn;

        @Override
        public CommandResult execute(CommandInvocation inv) {
            capture(inv);
            return CommandResult.SUCCESS;
        }
    }

    private CommandRuntime<CommandInvocation> runtime(Class<? extends Command> command) throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(command).create();
        return AeshCommandRuntimeBuilder.<CommandInvocation> builder().commandRegistry(registry).build();
    }

    @Test
    public void testAbsentOptionsAreNotMatched() throws Exception {
        CommandResult result = runtime(MatchCommand.class).executeCommand("match");
        assertEquals(CommandResult.SUCCESS, result);

        assertFalse(dirMatched);
        assertTrue(dirValues.isEmpty());
        assertFalse(levelMatched);
        assertTrue(levelValues.isEmpty());
        assertFalse(portMatched);
        assertFalse(verboseMatched);
        assertFalse(findMatched);
        assertTrue(findValues.isEmpty());
        assertFalse("provider default must not count as given", dynMatched);
        assertTrue(matchedNames.isEmpty());
    }

    @Test
    public void testExplicitValueEqualToDefaultStillMatches() throws Exception {
        // The Camel case: the user typed the value even though it equals the default.
        CommandResult result = runtime(MatchCommand.class)
                .executeCommand("match --dir x --level INFO --port 8080 -v");
        assertEquals(CommandResult.SUCCESS, result);

        assertTrue(dirMatched);
        assertEquals(Arrays.asList("x"), dirValues);
        assertTrue(levelMatched);
        assertEquals(Arrays.asList("INFO"), levelValues);
        assertTrue(portMatched);
        assertEquals(Arrays.asList("8080"), portValues);
        assertTrue(verboseMatched);
        assertEquals(Arrays.asList("true"), verboseValues);
        assertEquals(new HashSet<>(Arrays.asList("dir", "level", "port", "verbose")), matchedNames);
    }

    @Test
    public void testBareOptionalListMatchesWithEmptyValues() throws Exception {
        CommandResult result = runtime(MatchCommand.class).executeCommand("match --find");
        assertEquals(CommandResult.SUCCESS, result);

        assertTrue(findMatched);
        assertTrue(findValues.isEmpty());
        assertTrue(matchedNames.contains("find"));
    }

    @Test
    public void testGroupParentOptionMatchedFromChild() throws Exception {
        ChildMatchCommand.ran = false;
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(GroupMatchCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(registry).build();

        CommandResult result = runtime.executeCommand("gmatch --global g child --name c");
        assertEquals(CommandResult.SUCCESS, result);
        assertTrue(ChildMatchCommand.ran);
        assertTrue(ChildMatchCommand.globalMatched);
        assertEquals(Arrays.asList("g"), ChildMatchCommand.globalValues);
        assertTrue(ChildMatchCommand.nameMatched);
        assertEquals(Arrays.asList("c"), ChildMatchCommand.nameValues);
    }

    @CommandDefinition(name = "gmatch", description = "group match", groupCommands = { ChildMatchCommand.class })
    public static class GroupMatchCommand implements Command<CommandInvocation> {
        @Option(name = "global", description = "global")
        String global;

        @Override
        public CommandResult execute(CommandInvocation inv) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child", description = "child match")
    public static class ChildMatchCommand implements Command<CommandInvocation> {
        static boolean ran;
        static boolean globalMatched;
        static List<String> globalValues;
        static boolean nameMatched;
        static List<String> nameValues;

        @Option(name = "name", description = "name")
        String name;

        @Override
        public CommandResult execute(CommandInvocation inv) {
            ran = true;
            globalMatched = inv.hasMatchedOption("--global");
            globalValues = inv.matchedOptionValues("global");
            nameMatched = inv.hasMatchedOption("name");
            nameValues = inv.matchedOptionValues("name");
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testAliasMatches() throws Exception {
        AliasMatchCommand.ran = false;
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(AliasMatchCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(registry).build();

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("aliasmatch --mode fast"));
        assertTrue(AliasMatchCommand.ran);
        assertTrue(AliasMatchCommand.shortMatched);
        assertEquals(Arrays.asList("fast"), AliasMatchCommand.shortValues);
    }

    @CommandDefinition(name = "aliasmatch", description = "alias match")
    public static class AliasMatchCommand implements Command<CommandInvocation> {
        static boolean ran;
        static boolean shortMatched;
        static List<String> shortValues;

        @Option(name = "mode", aliases = { "m" }, description = "mode")
        String mode;

        @Override
        public CommandResult execute(CommandInvocation inv) {
            ran = true;
            shortMatched = inv.hasMatchedOption("m");
            shortValues = inv.matchedOptionValues("-m");
            return CommandResult.SUCCESS;
        }
    }
}
