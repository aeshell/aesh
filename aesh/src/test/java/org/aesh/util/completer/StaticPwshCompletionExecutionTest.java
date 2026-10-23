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
package org.aesh.util.completer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.Option;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.console.DefaultAeshContext;
import org.aesh.terminal.formatting.TerminalString;
import org.aesh.util.completer.ShellCompletionGenerator.ShellType;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Executes generated static PowerShell completion scripts in a real pwsh via
 * {@code TabExpansion2} and asserts the offered candidates. Nested
 * subcommands must resolve through the full command path: every group level
 * scans for its own children and dispatches deeper (#671).
 */
public class StaticPwshCompletionExecutionTest {

    @CommandDefinition(name = "leaf", description = "leaf")
    public static class LeafCommand implements Command<CommandInvocation> {
        @Option(name = "leaf-only", description = "leaf only", aliases = { "lo" })
        String leafOnly;

        @Option(name = "mode", description = "mode", allowedValues = { "fast", "slow" })
        String mode;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mid", description = "mid", groupCommands = { LeafCommand.class })
    public static class MidCommand implements Command<CommandInvocation> {
        @Option(name = "mid-opt", description = "mid opt")
        String midOpt;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "cmd", description = "cmd", groupCommands = { MidCommand.class })
    public static class RootCommand implements Command<CommandInvocation> {
        @Option(name = "global", description = "global", inherited = true)
        String global;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @BeforeClass
    public static void requirePwsh() throws Exception {
        boolean available = false;
        try {
            Process probe = new ProcessBuilder("pwsh", "-NoProfile", "-NonInteractive",
                    "-Command", "$PSVersionTable.PSVersion.ToString()").start();
            available = probe.waitFor(60, TimeUnit.SECONDS) && probe.exitValue() == 0;
        } catch (Exception ignored) {
        }
        assumeTrue("pwsh is required for executable completion tests", available);
    }

    private static CommandLineParser<CommandInvocation> parser() throws Exception {
        return new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new RootCommand()).getParser();
    }

    private static String script() throws Exception {
        return ShellCompletionGenerator.forShell(ShellType.PWSH).generate(parser(), "cmd");
    }

    /**
     * Every input exercised below. All cases run in a single pwsh process
     * (spawning pwsh dominates test time); results are split per input.
     * Add new inputs here when adding tests.
     */
    private static final List<String> BATCH_INPUTS = Arrays.asList(
            "cmd mid leaf --leaf",
            "cmd --glo",
            "cmd mid --",
            "cmd mid leaf --",
            "cmd mid ",
            "cmd mid leaf --l",
            "cmd mid leaf --mode ");

    private static volatile List<List<String>> batchResults;

    /**
     * Sources the script in a real pwsh and asks the completion engine for
     * candidates at the end of the given input line, returning the offered
     * completion texts. Backed by a single batched pwsh run shared by all
     * tests in this class.
     */
    private static synchronized List<String> completeInPwsh(String script, String input)
            throws Exception {
        if (batchResults == null)
            batchResults = runBatch(script);
        int index = BATCH_INPUTS.indexOf(input);
        if (index < 0 || index >= batchResults.size())
            throw new IllegalStateException(
                    "No batched completion result for input [" + input + "]; add it to BATCH_INPUTS");
        return batchResults.get(index);
    }

    private static List<List<String>> runBatch(String script) throws Exception {
        File dir = Files.createTempDirectory("aesh-pwsh-complete").toFile();
        try {
            File scriptFile = new File(dir, "complete.ps1");
            Files.write(scriptFile.toPath(), script.getBytes(StandardCharsets.UTF_8));
            List<String> driver = new ArrayList<>();
            driver.add("param([string]$GenScript, [string]$LinesFile)");
            driver.add(". $GenScript");
            driver.add("$i = 0");
            driver.add("foreach ($Line in [System.IO.File]::ReadAllLines($LinesFile)) {");
            driver.add("    Write-Output (\"CASE:\" + $i)");
            driver.add("    $r = TabExpansion2 -inputScript $Line -cursorColumn $Line.Length");
            driver.add("    foreach ($m in $r.CompletionMatches) {");
            driver.add("        Write-Output (\"CAND:\" + $m.CompletionText)");
            driver.add("    }");
            driver.add("    $i = $i + 1");
            driver.add("}");
            File driverFile = new File(dir, "driver.ps1");
            Files.write(driverFile.toPath(),
                    String.join("\r\n", driver).getBytes(StandardCharsets.UTF_8));
            File linesFile = new File(dir, "lines.txt");
            Files.write(linesFile.toPath(),
                    String.join("\n", BATCH_INPUTS).getBytes(StandardCharsets.UTF_8));

            List<String> command = new ArrayList<>();
            command.add("pwsh");
            command.add("-NoProfile");
            command.add("-NonInteractive");
            command.add("-File");
            command.add(driverFile.getAbsolutePath());
            command.add(scriptFile.getAbsolutePath());
            command.add(linesFile.getAbsolutePath());
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("pwsh completion timed out");
            }
            String output = new String(readAll(process.getInputStream()),
                    StandardCharsets.UTF_8);
            if (process.exitValue() != 0)
                throw new IllegalStateException("pwsh failed: " + output);
            List<List<String>> buckets = new ArrayList<>();
            List<String> current = null;
            for (String line : output.split("\\r?\\n")) {
                if (line.startsWith("CASE:")) {
                    current = new ArrayList<>();
                    buckets.add(current);
                } else if (line.startsWith("CAND:") && current != null) {
                    current.add(line.substring("CAND:".length()));
                }
            }
            if (buckets.size() != BATCH_INPUTS.size())
                throw new IllegalStateException("Expected " + BATCH_INPUTS.size()
                        + " completion cases, got " + buckets.size() + ": " + output);
            return buckets;
        } finally {
            deleteRecursively(dir);
        }
    }

    private static byte[] readAll(java.io.InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = in.read(chunk)) != -1)
            buffer.write(chunk, 0, n);
        return buffer.toByteArray();
    }

    private static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children)
                deleteRecursively(child);
        }
        file.delete();
    }

    @Test
    public void testNestedLeafOptionCompleted() throws Exception {
        // The exact #671 repro: siblings' options used to leak in instead.
        List<String> candidates = completeInPwsh(script(), "cmd mid leaf --leaf");

        assertTrue("Leaf option must complete at depth, got: " + candidates,
                candidates.contains("--leaf-only"));
    }

    @Test
    public void testRootOptionsCompleted() throws Exception {
        List<String> candidates = completeInPwsh(script(), "cmd --glo");

        assertTrue("Root options must complete, got: " + candidates,
                candidates.contains("--global"));
    }

    @Test
    public void testMidOptionsCompleted() throws Exception {
        List<String> candidates = completeInPwsh(script(), "cmd mid --");

        assertTrue("Mid options must complete, got: " + candidates,
                candidates.contains("--mid-opt"));
        assertTrue("Inherited options must complete at depth, got: " + candidates,
                candidates.contains("--global"));
    }

    @Test
    public void testLeafOptionsWithInheritedCompleted() throws Exception {
        List<String> candidates = completeInPwsh(script(), "cmd mid leaf --");

        assertTrue("Leaf options must complete, got: " + candidates,
                candidates.contains("--leaf-only"));
        assertTrue("Inherited options must complete at the leaf, got: " + candidates,
                candidates.contains("--global"));
        assertFalse("Sibling options must not leak to the leaf, got: " + candidates,
                candidates.contains("--mid-opt"));
    }

    @Test
    public void testLeafSubcommandCompleted() throws Exception {
        List<String> candidates = completeInPwsh(script(), "cmd mid ");

        assertTrue("Leaf subcommand must complete, got: " + candidates,
                candidates.contains("leaf"));
    }

    @Test
    public void testAliasCompletedAtDepth() throws Exception {
        List<String> candidates = completeInPwsh(script(), "cmd mid leaf --l");

        assertTrue("Alias must complete at depth, got: " + candidates,
                candidates.contains("--lo"));
        assertTrue("Primary must complete at depth, got: " + candidates,
                candidates.contains("--leaf-only"));
    }

    @Test
    public void testOptionValuesCompletedAtDepth() throws Exception {
        List<String> candidates = completeInPwsh(script(), "cmd mid leaf --mode ");

        assertTrue("Allowed values must complete at depth, got: " + candidates,
                candidates.contains("fast"));
        assertTrue("Allowed values must complete at depth, got: " + candidates,
                candidates.contains("slow"));
    }

    @Test
    public void testStaticMatchesDynamicAtDepth() throws Exception {
        // Dynamic completion is the reference: same buffer through the
        // interactive completion path must offer the same option set.
        InvocationProviders providers = SettingsBuilder.builder().build().invocationProviders();
        String buffer = "cmd mid leaf --";
        AeshCompleteOperation operation = new AeshCompleteOperation(new DefaultAeshContext(),
                buffer, buffer.length());
        parser().complete(operation, providers);
        Set<String> dynamic = new HashSet<>();
        for (TerminalString candidate : operation.getCompletionCandidates())
            dynamic.add(candidate.getCharacters().trim());

        Set<String> staticCandidates = new HashSet<>(completeInPwsh(script(), buffer));

        assertEquals("Static and dynamic completion must agree at depth",
                dynamic, staticCandidates);
    }
}
