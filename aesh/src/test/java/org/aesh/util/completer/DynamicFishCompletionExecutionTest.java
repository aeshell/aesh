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
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.util.completer.ShellCompletionGenerator.ShellType;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Executes generated <em>dynamic</em> fish completion scripts in a real fish
 * via {@code complete --do-complete}. Fish matches bare {@code -a} candidates
 * against the whole token, so values after {@code --opt=} are filtered out
 * unless the option registers its own {@code -r} callback entry (#693). A
 * stub {@code somuch} executable answers the {@code --aesh-complete}
 * callbacks; the server side is covered by the parser unit tests.
 */
public class DynamicFishCompletionExecutionTest {

    @CommandDefinition(name = "address", description = "address")
    public static class AddressCommand implements Command<CommandInvocation> {
        @Option(name = "format", description = "Output format", allowedValues = { "text", "json", "sexp", "text0" })
        String format;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "somuch", description = "somuch", groupCommands = { AddressCommand.class })
    public static class SomuchCommand implements Command<CommandInvocation> {
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
                .create(new SomuchCommand()).getParser();
    }

    private static String script() throws Exception {
        return ShellCompletionGenerator.forShell(ShellType.FISH).generateDynamic(parser(), "somuch");
    }

    /**
     * Sources the generated script with a stub {@code somuch} on PATH and
     * asks fish for completions of the given line, returning the offered
     * completion texts. The stub answers value queries the way the real
     * callback would, so this exercises fish-side filtering and insertion.
     */
    private static List<String> completeInFish(String script, String input) throws Exception {
        File dir = Files.createTempDirectory("aesh-fish-dynamic-complete").toFile();
        try {
            Files.write(new File(dir, "complete.fish").toPath(), script.getBytes(StandardCharsets.UTF_8));
            File stub = new File(dir, "somuch");
            Files.write(stub.toPath(), ("#!/bin/sh\n"
                    + "# Fake somuch answering --aesh-complete value queries.\n"
                    + "for a in \"$@\"; do\n"
                    + "  case \"$a\" in\n"
                    + "    --format|--format=*) printf 'text\\njson\\nsexp\\ntext0\\n';;\n"
                    + "  esac\n"
                    + "done\n").getBytes(StandardCharsets.UTF_8));
            if (!stub.setExecutable(true))
                throw new IllegalStateException("Cannot make stub executable: " + stub);

            List<String> command = new ArrayList<>();
            command.add("fish");
            command.add("-c");
            command.add("set -gx PATH \"" + dir.getAbsolutePath() + "\" $PATH; "
                    + "source \"" + new File(dir, "complete.fish").getAbsolutePath() + "\"; "
                    + "complete --do-complete \"" + input.replace("\"", "\\\"") + "\"");
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("fish completion timed out");
            }
            String output = new String(readAll(process.getInputStream()), StandardCharsets.UTF_8);
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
    public void testEqualsSignValueCompletedAsFullOption() throws Exception {
        List<String> candidates = completeInFish(script(), "somuch address --format=");

        assertTrue("Value after '=' must be offered, got: " + candidates,
                candidates.contains("--format=text"));
        assertTrue("Value after '=' must be offered, got: " + candidates,
                candidates.contains("--format=json"));
        for (String candidate : candidates)
            assertTrue("Bare values must not leak through the '=' filter, got: " + candidate,
                    !candidate.equals("text") && !candidate.equals("json")
                            && !candidate.equals("sexp") && !candidate.equals("text0"));
    }

    @Test
    public void testSpaceSeparatedValueCompletedBare() throws Exception {
        List<String> candidates = completeInFish(script(), "somuch address --format ");

        assertTrue("Bare values must be offered after a space, got: " + candidates,
                candidates.contains("text"));
        assertTrue("Bare values must be offered after a space, got: " + candidates,
                candidates.contains("json"));
    }

    @Test
    public void testUnknownOptionValueYieldsNoCandidates() throws Exception {
        List<String> candidates = completeInFish(script(), "somuch address --bogus=");

        assertEquals("Unknown options must offer nothing, got: " + candidates,
                0, candidates.size());
    }
}
