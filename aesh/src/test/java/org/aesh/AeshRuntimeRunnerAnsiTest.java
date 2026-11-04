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
import org.aesh.command.invocation.CommandInvocation;
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
}
