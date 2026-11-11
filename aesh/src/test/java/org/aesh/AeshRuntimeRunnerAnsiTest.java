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
package org.aesh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.junit.Test;

/**
 * ANSI colors follow the terminal in {@code AeshRuntimeRunner}: on for a TTY,
 * off when stdout is redirected, unless forced on/off.
 */
public class AeshRuntimeRunnerAnsiTest {

    private static final String ESC = String.valueOf((char) 27);

    private static java.util.function.Function<String, String> env(Map<String, String> vars) {
        return vars::get;
    }

    private static Map<String, String> envOf(String... pairs) {
        Map<String, String> vars = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2)
            vars.put(pairs[i], pairs[i + 1]);
        return vars;
    }

    private static final Supplier<Boolean> TTY = () -> true;
    private static final Supplier<Boolean> PIPED = () -> false;

    @Test
    public void testOffNeverColors() {
        assertFalse(AeshRuntimeRunner.resolveAnsiColors(AeshRuntimeRunner.AnsiMode.OFF, TTY,
                env(Collections.emptyMap())));
        assertFalse(AeshRuntimeRunner.resolveAnsiColors(AeshRuntimeRunner.AnsiMode.OFF, PIPED,
                env(envOf("FORCE_COLOR", "1"))));
    }

    @Test
    public void testOnColorsUnlessNoColor() {
        assertTrue(AeshRuntimeRunner.resolveAnsiColors(AeshRuntimeRunner.AnsiMode.ON, PIPED,
                env(Collections.emptyMap())));
        assertFalse(AeshRuntimeRunner.resolveAnsiColors(AeshRuntimeRunner.AnsiMode.ON, TTY,
                env(envOf("NO_COLOR", ""))));
    }

    @Test
    public void testAutoFollowsStdout() {
        assertTrue(AeshRuntimeRunner.resolveAnsiColors(AeshRuntimeRunner.AnsiMode.AUTO, TTY,
                env(Collections.emptyMap())));
        assertFalse(AeshRuntimeRunner.resolveAnsiColors(AeshRuntimeRunner.AnsiMode.AUTO, PIPED,
                env(Collections.emptyMap())));
    }

    @Test
    public void testAutoForceColor() {
        assertTrue(AeshRuntimeRunner.resolveAnsiColors(AeshRuntimeRunner.AnsiMode.AUTO, PIPED,
                env(envOf("FORCE_COLOR", "1"))));
        assertTrue(AeshRuntimeRunner.resolveAnsiColors(AeshRuntimeRunner.AnsiMode.AUTO, PIPED,
                env(envOf("CLICOLOR_FORCE", "1"))));
        // "0" does not force: falls back to the terminal probe
        assertFalse(AeshRuntimeRunner.resolveAnsiColors(AeshRuntimeRunner.AnsiMode.AUTO, PIPED,
                env(envOf("FORCE_COLOR", "0"))));
    }

    @Test
    public void testAutoNoColorWins() {
        assertFalse(AeshRuntimeRunner.resolveAnsiColors(AeshRuntimeRunner.AnsiMode.AUTO, TTY,
                env(envOf("NO_COLOR", "", "FORCE_COLOR", "1"))));
    }

    @Test
    public void testForceWhenRedirected() {
        assertTrue(AeshRuntimeRunner.resolveForceWhenRedirected(AeshRuntimeRunner.AnsiMode.ON,
                env(Collections.emptyMap())));
        assertTrue(AeshRuntimeRunner.resolveForceWhenRedirected(AeshRuntimeRunner.AnsiMode.AUTO,
                env(envOf("FORCE_COLOR", "1"))));
        assertTrue(AeshRuntimeRunner.resolveForceWhenRedirected(AeshRuntimeRunner.AnsiMode.AUTO,
                env(envOf("CLICOLOR_FORCE", "yes"))));
        assertFalse(AeshRuntimeRunner.resolveForceWhenRedirected(AeshRuntimeRunner.AnsiMode.AUTO,
                env(Collections.emptyMap())));
        assertFalse(AeshRuntimeRunner.resolveForceWhenRedirected(AeshRuntimeRunner.AnsiMode.OFF,
                env(envOf("FORCE_COLOR", "1"))));
        assertFalse(AeshRuntimeRunner.resolveForceWhenRedirected(AeshRuntimeRunner.AnsiMode.ON,
                env(envOf("NO_COLOR", ""))));
        assertFalse(AeshRuntimeRunner.resolveForceWhenRedirected(AeshRuntimeRunner.AnsiMode.AUTO,
                env(envOf("FORCE_COLOR", "0"))));
    }

    @Test
    public void testAnsiModeResolveInstanceMethods() {
        // Instance methods are the public entry point for direct
        // CommandRuntime users; they must match the static decision table.
        assertFalse(AeshRuntimeRunner.AnsiMode.OFF.resolve(TTY, env(Collections.emptyMap())));
        assertFalse(AeshRuntimeRunner.AnsiMode.OFF.resolve(PIPED, env(envOf("FORCE_COLOR", "1"))));
        assertTrue(AeshRuntimeRunner.AnsiMode.ON.resolve(PIPED, env(Collections.emptyMap())));
        assertFalse(
                AeshRuntimeRunner.AnsiMode.ON.resolve(TTY, env(envOf("NO_COLOR", ""))));
        assertTrue(AeshRuntimeRunner.AnsiMode.AUTO.resolve(TTY, env(Collections.emptyMap())));
        assertFalse(AeshRuntimeRunner.AnsiMode.AUTO.resolve(PIPED, env(Collections.emptyMap())));
        assertTrue(AeshRuntimeRunner.AnsiMode.AUTO.resolve(PIPED, env(envOf("FORCE_COLOR", "1"))));
        assertFalse(AeshRuntimeRunner.AnsiMode.AUTO.resolve(TTY, env(envOf("NO_COLOR", "", "FORCE_COLOR", "1"))));

        assertTrue(AeshRuntimeRunner.AnsiMode.ON.resolveWhenRedirected(env(Collections.emptyMap())));
        assertTrue(AeshRuntimeRunner.AnsiMode.AUTO
                .resolveWhenRedirected(env(envOf("FORCE_COLOR", "1"))));
        assertFalse(AeshRuntimeRunner.AnsiMode.AUTO
                .resolveWhenRedirected(env(Collections.emptyMap())));
        assertFalse(AeshRuntimeRunner.AnsiMode.OFF
                .resolveWhenRedirected(env(envOf("FORCE_COLOR", "1"))));
    }

    @Test
    public void testRuntimeBuilderAnsiColorsAppliesToBuiltContainers() throws Exception {
        // Direct CommandRuntime users get the same no-colors rule by resolving
        // the mode once and passing the boolean -- no runner required (#767).
        boolean colors = AeshRuntimeRunner.AnsiMode.AUTO.resolve(PIPED, env(Collections.emptyMap()));
        assertFalse(colors);
        org.aesh.command.CommandRuntime<CommandInvocation> runtime = org.aesh.command.AeshCommandRuntimeBuilder
                .<CommandInvocation> builder()
                .commandRegistry(org.aesh.command.impl.registry.AeshCommandRegistryBuilder
                        .<CommandInvocation> builder().command(PaintCommand.class).create())
                .ansiColors(colors)
                .build();

        assertFalse(runtime.getCommandRegistry().getCommand("paint", "").getParser().isAnsiMode());
        String help = runtime.getCommandRegistry().getCommand("paint", "").getParser().printHelp();
        assertFalse("Help must be plain, got: " + help, help.contains(ESC));
    }

    @Test
    public void testRuntimeBuilderAnsiColorsAppliesToLaterBuilds() throws Exception {
        // Deferred class registrations materialize after build(); the stored
        // flag must reach them too.
        org.aesh.command.impl.registry.AeshCommandRegistryBuilder<CommandInvocation> registryBuilder = org.aesh.command.impl.registry.AeshCommandRegistryBuilder
                .<CommandInvocation> builder().command(PaintCommand.class);
        org.aesh.command.CommandRuntime<CommandInvocation> runtime = org.aesh.command.AeshCommandRuntimeBuilder
                .<CommandInvocation> builder()
                .commandRegistry(registryBuilder.create())
                .ansiColors(false)
                .build();

        assertFalse(runtime.getCommandRegistry().getCommand("paint", "").getParser().isAnsiMode());
    }

    @Test
    public void testRuntimeBuilderDefaultsToRegistryMode() throws Exception {
        // Unset leaves the registry alone (colors on, modulo NO_COLOR).
        org.aesh.command.CommandRuntime<CommandInvocation> runtime = org.aesh.command.AeshCommandRuntimeBuilder
                .<CommandInvocation> builder()
                .commandRegistry(org.aesh.command.impl.registry.AeshCommandRegistryBuilder
                        .<CommandInvocation> builder().command(PaintCommand.class).create())
                .build();

        assumeTrue(System.getenv("NO_COLOR") == null);
        assertTrue(runtime.getCommandRegistry().getCommand("paint", "").getParser().isAnsiMode());
    }

    @Test
    public void testAutoWhenPipedHasNoAnsi() {
        // Surefire pipes stdout, so AUTO resolves to plain text here
        assumeCleanEnv();
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(PaintCommand.class)
                .args("--help")
                .execute());

        assertFalse("Redirected help must be plain text, got: " + output, output.contains(ESC));
    }

    @Test
    public void testOnForcesAnsiWhenPiped() {
        // NO_COLOR would win over ON by design; skip there
        assumeTrue(System.getenv("NO_COLOR") == null);
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(PaintCommand.class)
                .ansiMode(AeshRuntimeRunner.AnsiMode.ON)
                .args("--help")
                .execute());

        assertTrue("Forced colors must survive redirection, got: " + output, output.contains(ESC));
    }

    @Test
    public void testOffDropsAnsi() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(PaintCommand.class)
                .ansiMode(AeshRuntimeRunner.AnsiMode.OFF)
                .args("--help")
                .execute());

        assertFalse("Disabled colors must stay off, got: " + output, output.contains(ESC));
    }

    @Test
    public void testUnknownSubcommandHelpRespectsMode() {
        assumeTrue(System.getenv("NO_COLOR") == null);
        String stderr = captureStderr(() -> AeshRuntimeRunner.builder()
                .command(GrpCommand.class)
                .ansiMode(AeshRuntimeRunner.AnsiMode.ON)
                .args("bogus")
                .execute());

        assertTrue("Fail-fast help must honor forced colors, got: " + stderr, stderr.contains(ESC));
    }

    @Test
    public void testLazySubcommandHelpHasNoAnsiWhenPiped() {
        // Surefire pipes stdout, so AUTO resolves to plain text here; the
        // lazily resolved subcommand must inherit the root's mode (#753)
        assumeCleanEnv();
        AeshCommandContainerBuilder<CommandInvocation> containerBuilder = new AeshCommandContainerBuilder<>();
        containerBuilder.setLazyChildResolution(true);
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .containerBuilder(containerBuilder)
                .command(GrpCommand.class)
                .lazyStartup(false)
                .args("paint", "--help")
                .execute());

        assertFalse("Lazy subcommand help must be plain when piped, got: " + output, output.contains(ESC));
    }

    @Test
    public void testLazyGrandchildrenInheritNoColorMode() throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        builder.setLazyChildResolution(true);
        CommandLineParser<CommandInvocation> root = builder.create(new AnsiRootGroup()).getParser();
        // The registry applies no-color after registration but before
        // lazy children resolve; attach must propagate it (#753)
        root.updateAnsiMode(false);

        CommandLineParser<CommandInvocation> mid = findChild(root, "ansi-mid");
        CommandLineParser<CommandInvocation> leaf = findChild(mid, "ansi-leaf");

        String midHelp = mid.printHelp();
        assertFalse("Lazy child help must be plain, got: " + midHelp, midHelp.contains(ESC));
        String leafHelp = leaf.printHelp();
        assertFalse("Lazy grandchild help must be plain, got: " + leafHelp, leafHelp.contains(ESC));
    }

    private static CommandLineParser<CommandInvocation> findChild(CommandLineParser<CommandInvocation> parent,
            String name) {
        for (CommandLineParser<CommandInvocation> child : parent.getAllChildParsers()) {
            if (child.getProcessedCommand().name().equals(name))
                return child;
        }
        throw new AssertionError("No child named " + name);
    }

    @Test
    public void testLazyHelpOverloads() throws Exception {
        String plain = org.aesh.command.impl.LazyHelp.renderUnknownSubcommandHelp(
                GrpCommand.class, new String[] { "bogus" }, false);
        assertFalse("Explicit off must be plain, got: " + plain, plain.contains(ESC));

        String colored = org.aesh.command.impl.LazyHelp.renderUnknownSubcommandHelp(
                GrpCommand.class, new String[] { "bogus" }, true);
        assertTrue("Explicit on must color, got: " + colored, colored.contains(ESC));
    }

    @Test
    public void testUsageErrorHelpRespectsMode() {
        assumeTrue(System.getenv("NO_COLOR") == null);
        CommandResult[] result = new CommandResult[1];
        String stderr = captureStderr(() -> result[0] = AeshRuntimeRunner.builder()
                .command(PaintCommand.class)
                .lazyStartup(false)
                .ansiMode(AeshRuntimeRunner.AnsiMode.ON)
                .args("--bogus", "x")
                .execute());

        assertEquals(CommandResult.USAGE_ERROR, result[0]);
        assertTrue("Usage-error help must honor forced colors, got: " + stderr, stderr.contains(ESC));
    }

    @Test
    public void testRedirectedExecutionHelpIsPlain() throws Exception {
        java.io.File tmp = java.io.File.createTempFile("aesh-ansi", ".txt");
        try {
            org.aesh.command.CommandRuntime<CommandInvocation> runtime = org.aesh.command.AeshCommandRuntimeBuilder
                    .<CommandInvocation> builder()
                    .commandRegistry(org.aesh.command.impl.registry.AeshCommandRegistryBuilder
                            .<CommandInvocation> builder().command(PaintCommand.class).create())
                    .operators(java.util.EnumSet.allOf(org.aesh.command.operator.OperatorType.class))
                    .build();

            assertEquals(CommandResult.SUCCESS,
                    runtime.executeCommand("paint --help > " + tmp.getAbsolutePath()));
            String content = new String(java.nio.file.Files.readAllBytes(tmp.toPath()));
            assertTrue("Help must land in the file, got: " + content, content.contains("Usage:"));
            assertFalse("Redirected help must be plain, got: " + content, content.contains(ESC));
        } finally {
            tmp.delete();
        }
    }

    @Test
    public void testRedirectedExecutionHelpForced() throws Exception {
        // Pipes preserve ANSI (the receiving command may be color-aware),
        // so forced colors survive there -- unlike file redirects, which
        // always strip them when writing.
        SinkDrainCommand.lines.clear();
        org.aesh.command.CommandRuntime<CommandInvocation> runtime = org.aesh.command.AeshCommandRuntimeBuilder
                .<CommandInvocation> builder()
                .commandRegistry(org.aesh.command.impl.registry.AeshCommandRegistryBuilder
                        .<CommandInvocation> builder().command(PaintCommand.class)
                        .command(SinkDrainCommand.class).create())
                .operators(java.util.EnumSet.allOf(org.aesh.command.operator.OperatorType.class))
                .ansiWhenRedirected(true)
                .build();

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("paint --help | sinkdrain"));
        assertTrue("Help must flow through the pipe, got: " + SinkDrainCommand.lines,
                SinkDrainCommand.lines.stream().anyMatch(l -> l.contains("Usage:")));
        assertTrue("Forced colors must survive the pipe, got: " + SinkDrainCommand.lines,
                SinkDrainCommand.lines.stream().anyMatch(l -> l.contains(ESC)));
    }

    @Test
    public void testPipedExecutionHelpIsPlain() throws Exception {
        SinkDrainCommand.lines.clear();
        org.aesh.command.CommandRuntime<CommandInvocation> runtime = org.aesh.command.AeshCommandRuntimeBuilder
                .<CommandInvocation> builder()
                .commandRegistry(org.aesh.command.impl.registry.AeshCommandRegistryBuilder
                        .<CommandInvocation> builder().command(PaintCommand.class)
                        .command(SinkDrainCommand.class).create())
                .operators(java.util.EnumSet.allOf(org.aesh.command.operator.OperatorType.class))
                .build();

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("paint --help | sinkdrain"));
        assertTrue("Help must flow through the pipe, got: " + SinkDrainCommand.lines,
                SinkDrainCommand.lines.stream().anyMatch(l -> l.contains("Usage:")));
        assertTrue("Piped help must be plain, got: " + SinkDrainCommand.lines,
                SinkDrainCommand.lines.stream().noneMatch(l -> l.contains(ESC)));
    }

    @Test
    public void testPrintSynopsisMatchesHelpUsageBlock() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new MultiCommand()).getParser();
        parser.updateAnsiMode(false);

        String synopsis = parser.printSynopsis(false);
        assertTrue("Synopsis starts with the Usage line, got: " + synopsis,
                synopsis.startsWith("Usage: multi"));
        assertFalse("Synopsis has no option list, got: " + synopsis,
                synopsis.contains("Options:"));
        assertFalse("Synopsis has no description, got: " + synopsis,
                synopsis.contains("Multi things"));
        assertTrue("Synopsis lists the options, got: " + synopsis,
                synopsis.contains("-a=") && synopsis.contains("--beta"));

        // The standalone block is byte-identical to the Usage block of --help
        assertTrue("Help contains the synopsis block verbatim",
                parser.printHelp().contains(synopsis));
    }

    @Test
    public void testPrintSynopsisCollapsed() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new MultiCommand()).getParser();
        parser.updateAnsiMode(false);

        String collapsed = parser.printSynopsis(true);
        assertTrue("Collapsed synopsis keeps the Usage line, got: " + collapsed,
                collapsed.startsWith("Usage: multi"));
        assertTrue("Collapsed synopsis folds options, got: " + collapsed,
                collapsed.contains("[OPTIONS]"));
        assertFalse("Collapsed synopsis drops the option list, got: " + collapsed,
                collapsed.contains("--beta") || collapsed.contains("-a="));
        // --help itself keeps the full list: the toggle is untouched
        assertTrue(parser.printHelp().contains("--alpha"));
    }

    @Test
    public void testPrintSynopsisAnsiMode() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new MultiCommand()).getParser();

        parser.updateAnsiMode(true);
        assertTrue("Colors on must color the synopsis",
                parser.printSynopsis(false).contains(ESC));
        parser.updateAnsiMode(false);
        assertFalse("Colors off must leave the synopsis plain, got: "
                + parser.printSynopsis(false),
                parser.printSynopsis(false).contains(ESC));
    }

    @Test
    public void testPrintSynopsisGroupMarker() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new GrpCommand()).getParser();
        parser.updateAnsiMode(false);

        assertTrue("Group synopsis marks the subcommand slot, got: " + parser.printSynopsis(false),
                parser.printSynopsis(false).contains("[COMMAND]"));
    }

    @Test
    public void testUsageErrorModeDefaultIsFullHelp() {
        assumeCleanEnv();
        CommandResult[] result = new CommandResult[1];
        String stderr = captureStderr(() -> result[0] = AeshRuntimeRunner.builder()
                .command(PaintCommand.class)
                .args("--bogus", "x")
                .execute());

        assertEquals(CommandResult.USAGE_ERROR, result[0]);
        assertTrue("Default keeps the full help, got: " + stderr, stderr.contains("Options:"));
        assertFalse("Default adds no hint, got: " + stderr, stderr.contains("Try '"));
    }

    @Test
    public void testUsageErrorModeUsage() {
        assumeCleanEnv();
        CommandResult[] result = new CommandResult[1];
        String stderr = captureStderr(() -> result[0] = AeshRuntimeRunner.builder()
                .command(PaintCommand.class)
                .usageErrorMode(AeshRuntimeRunner.UsageErrorMode.USAGE)
                .args("--bogus", "x")
                .execute());

        assertEquals(CommandResult.USAGE_ERROR, result[0]);
        assertTrue("Usage mode prints the Usage block, got: " + stderr, stderr.contains("Usage:"));
        assertFalse("Usage mode drops the option list, got: " + stderr, stderr.contains("Options:"));
        assertTrue("Usage mode hints at --help, got: " + stderr,
                stderr.contains("Try 'paint --help' for more information."));
    }

    @Test
    public void testUsageErrorModeCollapsed() {
        assumeCleanEnv();
        CommandResult[] result = new CommandResult[1];
        String stderr = captureStderr(() -> result[0] = AeshRuntimeRunner.builder()
                .command(MultiCommand.class)
                .usageErrorMode(AeshRuntimeRunner.UsageErrorMode.COLLAPSED_USAGE)
                .args("--bogus", "x")
                .execute());

        assertEquals(CommandResult.USAGE_ERROR, result[0]);
        assertTrue("Collapsed mode folds the options, got: " + stderr, stderr.contains("[OPTIONS]"));
        assertFalse("Collapsed mode drops the option list, got: " + stderr, stderr.contains("--beta"));
        assertTrue("Collapsed mode hints at --help, got: " + stderr,
                stderr.contains("Try 'multi --help' for more information."));
    }

    @Test
    public void testUsageErrorModeHintOnly() {
        assumeCleanEnv();
        CommandResult[] result = new CommandResult[1];
        String stderr = captureStderr(() -> result[0] = AeshRuntimeRunner.builder()
                .command(PaintCommand.class)
                .usageErrorMode(AeshRuntimeRunner.UsageErrorMode.HINT_ONLY)
                .args("--bogus", "x")
                .execute());

        assertEquals(CommandResult.USAGE_ERROR, result[0]);
        assertFalse("Hint-only mode prints no usage, got: " + stderr, stderr.contains("Usage:"));
        assertFalse("Hint-only mode prints no options, got: " + stderr, stderr.contains("Options:"));
        assertTrue("Hint-only mode hints at --help, got: " + stderr,
                stderr.contains("Try 'paint --help' for more information."));
    }

    @Test
    public void testUsageErrorModeUsageResolvesSubcommand() {
        assumeCleanEnv();
        CommandResult[] result = new CommandResult[1];
        String stderr = captureStderr(() -> result[0] = AeshRuntimeRunner.builder()
                .command(GrpCommand.class)
                .usageErrorMode(AeshRuntimeRunner.UsageErrorMode.USAGE)
                .args("paint", "--bogus", "x")
                .execute());

        assertEquals(CommandResult.USAGE_ERROR, result[0]);
        assertTrue("Group error prints the subcommand usage, got: " + stderr,
                stderr.contains("Usage: grp paint"));
        assertFalse("Group error drops the subcommand listing, got: " + stderr,
                stderr.contains("commands:"));
        assertTrue("Group error hints at the subcommand help, got: " + stderr,
                stderr.contains("Try 'grp paint --help' for more information."));
    }

    @CommandDefinition(name = "sinkdrain", description = "Drain stdin")
    public static class SinkDrainCommand implements Command<CommandInvocation> {
        static final java.util.List<String> lines = new java.util.ArrayList<>();

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            try {
                java.io.InputStream stdin = invocation.getStdin();
                if (stdin != null) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(stdin));
                    String line;
                    while ((line = reader.readLine()) != null)
                        lines.add(line);
                }
            } catch (Exception e) {
                return CommandResult.FAILURE;
            }
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testNonLazyHelpHasNoAnsiWhenPiped() {
        // Non-lazy mode materializes before execute(): already-built parsers
        // get the mode in create(), execution forks at fork time.
        assumeCleanEnv();
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(PaintCommand.class)
                .lazyStartup(false)
                .args("--help")
                .execute());

        assertFalse("Materialized help must be plain when piped, got: " + output, output.contains(ESC));
    }

    @Test
    public void testUsageErrorHelpHasNoAnsiWhenPiped() {
        // Usage errors print help from the materialized parsers
        assumeCleanEnv();
        CommandResult[] result = new CommandResult[1];
        String stderr = captureStderr(() -> result[0] = AeshRuntimeRunner.builder()
                .command(PaintCommand.class)
                .lazyStartup(false)
                .args("--bogus", "x")
                .execute());

        assertEquals(CommandResult.USAGE_ERROR, result[0]);
        assertFalse("Usage-error help must be plain when piped, got: " + stderr, stderr.contains(ESC));
    }

    private static void assumeCleanEnv() {
        assumeTrue(System.getenv("NO_COLOR") == null);
        assumeTrue(System.getenv("FORCE_COLOR") == null);
        assumeTrue(System.getenv("CLICOLOR_FORCE") == null);
    }

    private static String captureStdout(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return baos.toString();
    }

    private static String captureStderr(Runnable action) {
        PrintStream original = System.err;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setErr(new PrintStream(baos));
        try {
            action.run();
        } finally {
            System.setErr(original);
        }
        return baos.toString();
    }

    @CommandDefinition(name = "multi", description = "Multi things", generateHelp = true)
    public static class MultiCommand implements Command<CommandInvocation> {
        @Option(shortName = 'a', description = "Alpha value")
        String alpha;
        @Option(description = "Beta value")
        String beta;
        @Option(description = "Gamma value")
        String gamma;
        @Argument(description = "The target")
        String target;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "paint", description = "Paint things", generateHelp = true)
    public static class PaintCommand implements Command<CommandInvocation> {
        @Option(description = "The color", defaultValue = "red")
        String color;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "grp", description = "A group", generateHelp = true, groupCommands = { PaintCommand.class })
    public static class GrpCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "ansi-leaf", description = "Leaf", generateHelp = true)
    public static class AnsiLeafCommand implements Command<CommandInvocation> {
        @Option(description = "A value")
        String value;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "ansi-mid", description = "Mid", generateHelp = true, groupCommands = { AnsiLeafCommand.class })
    public static class AnsiMidGroup implements Command<CommandInvocation> {
        @Option(description = "Mid value")
        String mid;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "ansi-root", description = "Root", generateHelp = true, groupCommands = { AnsiMidGroup.class })
    public static class AnsiRootGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
