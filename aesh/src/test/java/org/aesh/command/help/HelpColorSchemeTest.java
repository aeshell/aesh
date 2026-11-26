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
package org.aesh.command.help;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aesh.AeshRuntimeRunner;
import org.aesh.AeshRuntimeRunner.AnsiMode;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.HelpColorScheme;
import org.aesh.command.HelpColorScheme.Style;
import org.aesh.command.HelpEntry;
import org.aesh.command.HelpSectionProvider;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.AeshCommandLineParser;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.junit.After;
import org.junit.Test;

/**
 * Verifies default help colors (bold sub-commands, combined bold+color on
 * required options) and customizable {@link HelpColorScheme} resolution and
 * propagation (#788).
 */
public class HelpColorSchemeTest {

    private static final String RESET = "\u001B[0m";

    @After
    public void cleanupGlobalState() {
        HelpColorScheme.setDefault(null);
        System.clearProperty(HelpColorScheme.PROP_COMMANDS);
        System.clearProperty(HelpColorScheme.PROP_OPTIONS);
        System.clearProperty(HelpColorScheme.PROP_PARAMETERS);
        System.clearProperty(HelpColorScheme.PROP_OPTION_PARAMS);
        System.clearProperty(HelpColorScheme.PROP_REQUIRED);
    }

    @CommandDefinition(name = "leaf", description = "Leaf command", generateHelp = true)
    public static class LeafCommand implements Command<CommandInvocation> {
        @Option(shortName = 't', name = "target", required = true, description = "Target env")
        String target;

        @Option(shortName = 'o', name = "output", description = "Output path")
        String output;

        @Argument(required = true, description = "Input file")
        String input;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class ExtraSectionProvider implements HelpSectionProvider {
        @Override
        public Map<String, List<HelpEntry>> getAdditionalSections() {
            return Collections.singletonMap("Plugins",
                    Collections.singletonList(new HelpEntry("lint", "Run lint checks")));
        }
    }

    @CommandDefinition(name = "root", description = "Root CLI", generateHelp = true, groupCommands = {
            LeafCommand.class }, helpSectionProvider = ExtraSectionProvider.class)
    public static class RootCommand implements Command<CommandInvocation> {
        @Option(shortName = 'v', hasValue = false, description = "Verbose")
        boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testDefaultsUseBoldSubcommandsAndMergedRequiredSgr() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(RootCommand.class).getParser();
        parser.updateAnsiMode(true);

        String rootHelp = parser.printHelp();
        assertTrue("Usage command should be bold:\n" + rootHelp,
                rootHelp.contains("Usage: \u001B[1mroot" + RESET));
        assertTrue("Sub-command name should be bold:\n" + rootHelp,
                rootHelp.contains("    \u001B[1mleaf" + RESET));
        assertTrue("HelpSectionProvider entry name should be bold:\n" + rootHelp,
                rootHelp.contains("    \u001B[1mlint" + RESET));
        assertFalse("Sub-command listing must not emit dark ANSI blue:\n" + rootHelp,
                rootHelp.contains("\u001B[34m") || rootHelp.contains("\u001B[0;34m"));

        CommandLineParser<CommandInvocation> leafParser = parser.getChildParser("leaf");
        String leafHelp = leafParser.printHelp();
        assertTrue("Required option must combine bold+yellow in one SGR after indent:\n" + leafHelp,
                leafHelp.contains("  \u001B[1;33m-t, --target" + RESET + "\u001B[36m=<target>" + RESET));
        assertTrue("Optional option should use plain yellow:\n" + leafHelp,
                leafHelp.contains("  \u001B[33m-o, --output" + RESET + "\u001B[36m=<output>" + RESET));
        assertTrue("Required positional must combine bold+cyan in one SGR:\n" + leafHelp,
                leafHelp.contains("  \u001B[1;36m<input>" + RESET));
        assertFalse("Must not emit redundant BOLD_OFF:\n" + leafHelp,
                leafHelp.contains("\u001B[0;22m"));
    }

    @Test
    public void testBuilderAndStringStyleParsing() {
        HelpColorScheme scheme = HelpColorScheme.builder()
                .commands(Style.BOLD, Style.BRIGHT_BLUE)
                .options("green")
                .parameters("fg(208)")
                .optionParams("#ff8800,italic")
                .required("bold,underline,red")
                .build();

        assertEquals("\u001B[1;94m", scheme.commandsEscape());
        assertEquals("\u001B[32m", scheme.optionsEscape());
        assertEquals("\u001B[38;5;208m", scheme.parametersEscape());
        assertEquals("\u001B[3;38;2;255;136;0m", scheme.optionParamsEscape());
        assertEquals("\u001B[1;4;31m", scheme.requiredOptionEscape());
        assertEquals("\u001B[1;4;31m", scheme.requiredParameterEscape());

        HelpColorScheme noReq = HelpColorScheme.builder()
                .options(Style.BLUE)
                .required(Style.NONE)
                .build();
        assertEquals("\u001B[34m", noReq.requiredOptionEscape());

        assertThrows(IllegalArgumentException.class,
                () -> HelpColorScheme.builder().options("not-a-color"));
        assertThrows(IllegalArgumentException.class,
                () -> HelpColorScheme.builder().options("fg(999)"));
        assertThrows(IllegalArgumentException.class,
                () -> HelpColorScheme.builder().options("#xyz"));
    }

    @Test
    public void testResolveFastPathReturnsSameInstanceWhenNoOverrides() {
        HelpColorScheme custom = HelpColorScheme.builder().options(Style.BLUE).build();
        assertSame(custom, HelpColorScheme.resolve(custom));
        assertSame(HelpColorScheme.defaults(), HelpColorScheme.resolve(null));
    }

    @Test
    public void testResolvePrecedenceAndLenientUserOverrides() {
        HelpColorScheme appScheme = HelpColorScheme.builder()
                .commands(Style.GREEN)
                .options(Style.BLUE)
                .parameters(Style.MAGENTA)
                .optionParams(Style.CYAN)
                .required(Style.UNDERLINE)
                .build();

        Map<String, String> env = new HashMap<>();
        env.put(HelpColorScheme.ENV_HELP_COLORS,
                "options=red:option-params=bright_magenta:unknown=bogus:parameters=invalid_token");

        Map<String, String> props = new HashMap<>();
        props.put(HelpColorScheme.PROP_OPTIONS, "bold,blue");
        props.put(HelpColorScheme.PROP_REQUIRED, "not-valid");

        HelpColorScheme resolved = HelpColorScheme.resolve(appScheme, env::get, props::get);
        assertEquals("\u001B[32m", resolved.commandsEscape()); // kept from appScheme
        assertEquals("\u001B[1;34m", resolved.optionsEscape()); // sysprop beat env
        assertEquals("\u001B[35m", resolved.parametersEscape()); // invalid env token ignored -> kept appScheme
        assertEquals("\u001B[95m", resolved.optionParamsEscape()); // env beat appScheme
        assertEquals("\u001B[1;4;34m", resolved.requiredOptionEscape()); // invalid sysprop ignored -> kept underline + bold,blue
    }

    @Test
    public void testSystemPropertiesOverrideHelpColorsAndAnsiOffStillWins() throws Exception {
        System.setProperty(HelpColorScheme.PROP_COMMANDS, "green");
        System.setProperty(HelpColorScheme.PROP_OPTIONS, "blue");
        System.setProperty(HelpColorScheme.PROP_OPTION_PARAMS, "magenta");
        System.setProperty(HelpColorScheme.PROP_REQUIRED, "underline");

        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(LeafCommand.class).getParser();
        parser.updateAnsiMode(true);

        String help = parser.printHelp();
        assertTrue("Commands sysprop applied:\n" + help,
                help.contains("Usage: \u001B[32mleaf" + RESET));
        assertTrue("Options + required sysprops merged:\n" + help,
                help.contains("  \u001B[4;34m-t, --target" + RESET + "\u001B[35m=<target>" + RESET));

        parser.updateAnsiMode(false);
        String plain = parser.printHelp();
        assertFalse("updateAnsiMode(false) must win over system properties:\n" + plain,
                plain.contains("\u001B["));
    }

    @Test
    public void testDirectParserSchemePropagatesToLazyChildren() throws Exception {
        // Mirrors Camel's usage: direct AeshCommandLineParser tree with lazy
        // sub-commands, no AeshRuntimeRunner.
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        builder.setLazyChildResolution(true);
        AeshCommandLineParser<CommandInvocation> rootParser = (AeshCommandLineParser<CommandInvocation>) builder
                .create(RootCommand.class).getParser();
        HelpColorScheme light = HelpColorScheme.builder()
                .commands(Style.BOLD, Style.BLACK)
                .options(Style.BLUE)
                .optionParams(Style.MAGENTA)
                .parameters(Style.MAGENTA)
                .required(Style.BOLD)
                .build();
        rootParser.updateAnsiMode(true);
        rootParser.updateHelpColorScheme(light);

        // Lazy child resolves directly via getChildParser (e.g. `camel leaf --help`)
        // BEFORE rootParser.printHelp() ever runs — proves addChildParser propagates it.
        String leafHelp = rootParser.getChildParser("leaf").printHelp();
        assertTrue("Lazy child help uses inherited options + required scheme:\n" + leafHelp,
                leafHelp.contains("  \u001B[1;34m-t, --target" + RESET + "\u001B[35m=<target>" + RESET));
        assertTrue("Lazy child help uses inherited positional scheme:\n" + leafHelp,
                leafHelp.contains("  \u001B[1;35m<input>" + RESET));

        String rootHelp = rootParser.printHelp();
        assertTrue("Root command uses custom scheme:\n" + rootHelp,
                rootHelp.contains("Usage: \u001B[1;30mroot" + RESET));
        assertTrue("Lazily resolved sub-command inherits custom scheme:\n" + rootHelp,
                rootHelp.contains("    \u001B[1;30mleaf" + RESET));
        assertTrue("HelpSectionProvider entry uses custom scheme:\n" + rootHelp,
                rootHelp.contains("    \u001B[1;30mlint" + RESET));
    }

    @Test
    public void testProcessWideDefaultAppliesWithoutWalkingParsers() throws Exception {
        HelpColorScheme.setDefault(HelpColorScheme.builder()
                .commands(Style.CYAN)
                .options(Style.GREEN)
                .build());

        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(RootCommand.class).getParser();
        parser.updateAnsiMode(true);
        String help = parser.printHelp();
        assertTrue("Process default commands style applied:\n" + help,
                help.contains("Usage: \u001B[36mroot" + RESET));
        assertTrue("Process default sub-command style applied:\n" + help,
                help.contains("    \u001B[36mleaf" + RESET));
        assertTrue("Process default options style applied:\n" + help,
                help.contains("\u001B[32m-v, --verbose" + RESET));
    }

    @Test
    public void testRegistryBuilderAndRuntimeRunnerApplyHelpColorScheme() throws Exception {
        HelpColorScheme custom = HelpColorScheme.builder()
                .commands(Style.MAGENTA)
                .options(Style.BLUE)
                .build();

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .helpColorScheme(custom)
                .command(RootCommand.class)
                .create();
        CommandLineParser<CommandInvocation> parser = registry.getCommand("root", "").getParser();
        parser.updateAnsiMode(true);
        assertTrue(parser.printHelp().contains("Usage: \u001B[35mroot" + RESET));

        // Both lazy (LazyHelp) and non-lazy AeshRuntimeRunner paths
        for (boolean lazy : new boolean[] { true, false }) {
            String out = captureStdout(() -> AeshRuntimeRunner.builder()
                    .lazyStartup(lazy)
                    .ansiMode(AnsiMode.ON)
                    .helpColorScheme(custom)
                    .command(RootCommand.class)
                    .args(new String[] { "--help" })
                    .execute());
            assertTrue("lazy=" + lazy + " must apply custom command color:\n" + out,
                    out.contains("Usage: \u001B[35mroot" + RESET));
            assertTrue("lazy=" + lazy + " must apply custom sub-command color:\n" + out,
                    out.contains("    \u001B[35mleaf" + RESET));
            assertTrue("lazy=" + lazy + " must apply custom option color:\n" + out,
                    out.contains("\u001B[34m-v, --verbose" + RESET));
        }
    }

    private static String captureStdout(Runnable action) {
        PrintStream prev = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(baos));
            action.run();
        } finally {
            System.setOut(prev);
        }
        return baos.toString();
    }
}
