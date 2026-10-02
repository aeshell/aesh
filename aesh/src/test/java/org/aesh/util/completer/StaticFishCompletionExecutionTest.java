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
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.console.DefaultAeshContext;
import org.aesh.terminal.formatting.TerminalString;
import org.aesh.util.completer.ShellCompletionGenerator.ShellType;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Executes generated static fish completion scripts in a real fish via
 * {@code complete --do-complete} and asserts the offered candidates.
 * Positional completion must be argument-aware: a file-typed argument
 * offers path completion, while a plain argument offers the eligible
 * option forms instead of stray filenames.
 */
public class StaticFishCompletionExecutionTest {

    @CommandDefinition(name = "sub", description = "sub")
    public static class SubCommand implements Command<CommandInvocation> {
        @Option(name = "mode", description = "mode", allowedValues = { "fast", "slow" })
        String mode;

        @Argument(description = "target name")
        String target;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "subf", description = "subf")
    public static class SubFileCommand implements Command<CommandInvocation> {
        @Argument(description = "target file")
        java.io.File target;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "cmd", description = "cmd", groupCommands = { SubCommand.class, SubFileCommand.class })
    public static class RootCommand implements Command<CommandInvocation> {
        @Option(name = "global", description = "global", inherited = true)
        String global;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @BeforeClass
    public static void requireFish() throws Exception {
        boolean available = false;
        try {
            Process probe = new ProcessBuilder("fish", "--version").start();
            available = probe.waitFor(10, TimeUnit.SECONDS) && probe.exitValue() == 0;
        } catch (Exception ignored) {
        }
        assumeTrue("fish is required for executable completion tests", available);
    }

    private static CommandLineParser<CommandInvocation> parser() throws Exception {
        return new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new RootCommand()).getParser();
    }

    private static String script() throws Exception {
        return ShellCompletionGenerator.forShell(ShellType.FISH).generate(parser(), "cmd");
    }

    /**
     * Sources the script in a real fish with the given working directory and
     * asks the completion engine for candidates for the given line, returning
     * the offered completion names.
     */
    private static List<String> completeInFish(String script, File workDir, String input)
            throws Exception {
        File dir = Files.createTempDirectory("aesh-fish-complete").toFile();
        try {
            File scriptFile = new File(dir, "complete.fish");
            Files.write(scriptFile.toPath(), script.getBytes(StandardCharsets.UTF_8));

            List<String> command = new ArrayList<>();
            command.add("fish");
            command.add("-c");
            command.add("source \"" + scriptFile.getAbsolutePath()
                    + "\"; complete --do-complete \"" + input.replace("\"", "\\\"") + "\"");
            Process process = new ProcessBuilder(command)
                    .directory(workDir)
                    .redirectErrorStream(true).start();
            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("fish completion timed out");
            }
            String output = new String(readAll(process.getInputStream()),
                    StandardCharsets.UTF_8);
            if (process.exitValue() != 0)
                throw new IllegalStateException("fish failed: " + output);
            List<String> candidates = new ArrayList<>();
            for (String line : output.split("\\r?\\n")) {
                int tab = line.indexOf('\t');
                String candidate = (tab < 0 ? line : line.substring(0, tab)).trim();
                if (!candidate.isEmpty())
                    candidates.add(candidate);
            }
            return candidates;
        } finally {
            deleteRecursively(dir);
        }
    }

    private static File workDirWithFiles() throws Exception {
        File dir = Files.createTempDirectory("aesh-fish-work").toFile();
        Files.write(new File(dir, "alpha.txt").toPath(), new byte[0]);
        Files.write(new File(dir, "beta.log").toPath(), new byte[0]);
        return dir;
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
    public void testFileArgumentCompleted() throws Exception {
        File workDir = workDirWithFiles();
        try {
            List<String> candidates = completeInFish(script(), workDir, "cmd subf ");

            assertEquals("File argument must offer exactly the directory files",
                    new HashSet<>(java.util.Arrays.asList("alpha.txt", "beta.log")),
                    new HashSet<>(candidates));
        } finally {
            deleteRecursively(workDir);
        }
    }

    @Test
    public void testFileCompletionIsExplicit() throws Exception {
        assertTrue("File arguments must use explicit path completion, got: " + script(),
                script().contains("__fish_complete_path"));
    }

    @Test
    public void testNonFileArgumentOffersOptionsNotFiles() throws Exception {
        // Files exist in the working directory on purpose: they must not leak
        // into a plain argument position.
        File workDir = workDirWithFiles();
        try {
            List<String> candidates = completeInFish(script(), workDir, "cmd sub ");

            assertTrue("Options must be offered at a plain argument, got: " + candidates,
                    candidates.contains("--mode"));
            assertTrue("Inherited options must be offered at a plain argument, got: "
                    + candidates, candidates.contains("--global"));
            assertFalse("Filenames must not leak into a plain argument, got: " + candidates,
                    candidates.contains("alpha.txt") || candidates.contains("beta.log"));
        } finally {
            deleteRecursively(workDir);
        }
    }

    @Test
    public void testSubOptionsCompleted() throws Exception {
        File workDir = workDirWithFiles();
        try {
            List<String> candidates = completeInFish(script(), workDir, "cmd sub --");

            assertTrue("Sub options must complete, got: " + candidates,
                    candidates.contains("--mode"));
            assertTrue("Inherited options must complete, got: " + candidates,
                    candidates.contains("--global"));
        } finally {
            deleteRecursively(workDir);
        }
    }

    @Test
    public void testSubcommandsCompleted() throws Exception {
        File workDir = workDirWithFiles();
        try {
            List<String> candidates = completeInFish(script(), workDir, "cmd s");

            assertTrue("Subcommands must complete, got: " + candidates,
                    candidates.contains("sub"));
            assertTrue("Subcommands must complete, got: " + candidates,
                    candidates.contains("subf"));
        } finally {
            deleteRecursively(workDir);
        }
    }

    @Test
    public void testOptionValuesCompleted() throws Exception {
        File workDir = workDirWithFiles();
        try {
            List<String> candidates = completeInFish(script(), workDir, "cmd sub --mode ");

            assertTrue("Allowed values must complete, got: " + candidates,
                    candidates.contains("fast"));
            assertTrue("Allowed values must complete, got: " + candidates,
                    candidates.contains("slow"));
            assertFalse("Options must not mix into value completion, got: " + candidates,
                    candidates.contains("--mode") || candidates.contains("--global"));
        } finally {
            deleteRecursively(workDir);
        }
    }

    @Test
    public void testStaticMatchesDynamicAtDepth() throws Exception {
        // Dynamic completion is the reference: same buffer through the
        // interactive completion path must offer the same option set.
        InvocationProviders providers = SettingsBuilder.builder().build().invocationProviders();
        String buffer = "cmd sub --";
        AeshCompleteOperation operation = new AeshCompleteOperation(new DefaultAeshContext(),
                buffer, buffer.length());
        parser().complete(operation, providers);
        Set<String> dynamic = new HashSet<>();
        for (TerminalString candidate : operation.getCompletionCandidates())
            dynamic.add(candidate.getCharacters().trim());

        File workDir = workDirWithFiles();
        try {
            Set<String> staticCandidates = new HashSet<>(
                    completeInFish(script(), workDir, buffer));

            assertEquals("Static and dynamic completion must agree at depth",
                    dynamic, staticCandidates);
        } finally {
            deleteRecursively(workDir);
        }
    }
}
