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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Executes generated static zsh completion scripts in a real zsh and asserts
 * the offered candidates. Nested subcommands must resolve through the full
 * command path: every group level generates its own function and dispatches
 * deeper (#670).
 *
 * <p>
 * The real {@code _arguments} can only run inside an interactive completion
 * context, so the driver stubs {@code _arguments}/{@code _describe} with the
 * dispatch-relevant contract: option words record their specs and set
 * {@code state=args}; the command word is consumed ({@code -C} normalization)
 * so {@code ${words[1]}} names the next subcommand; an empty positional slot
 * sets {@code state=cmd}. The generated script text under test is byte-real —
 * only the completion-system builtins are doubled. Interactive TAB completion
 * against the real {@code _arguments} verified the same scripts end to end
 * during development.
 */
public class StaticZshCompletionExecutionTest {

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
    public static void requireZsh() throws Exception {
        boolean available = false;
        try {
            Process probe = new ProcessBuilder("zsh", "--version").start();
            available = probe.waitFor(10, TimeUnit.SECONDS) && probe.exitValue() == 0;
        } catch (Exception ignored) {
        }
        assumeTrue("zsh is required for executable completion tests", available);
    }

    private static CommandLineParser<CommandInvocation> parser() throws Exception {
        return new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new RootCommand()).getParser();
    }

    private static String script() throws Exception {
        return ShellCompletionGenerator.forShell(ShellType.ZSH).generate(parser(), "cmd");
    }

    /**
     * Sources the generated script in a real zsh with the given words and
     * CURRENT (both 1-based, mirroring the completion system), returning the
     * stub-captured specs and descriptions.
     */
    private static String completeInZsh(String script, int current, String... words)
            throws Exception {
        File dir = Files.createTempDirectory("aesh-zsh-complete").toFile();
        try {
            File scriptFile = new File(dir, "complete.zsh");
            Files.write(scriptFile.toPath(), script.getBytes(StandardCharsets.UTF_8));
            File captureFile = new File(dir, "capture.txt");
            StringBuilder driver = new StringBuilder();
            driver.append("CAPTURE=\"").append(captureFile.getAbsolutePath()).append("\"\n");
            driver.append(": > \"$CAPTURE\"\n");
            driver.append("_arguments() {\n");
            driver.append("    print -r \"ARG-SPECS: $*\" >> \"$CAPTURE\"\n");
            driver.append("    local cur=\"\"\n");
            driver.append("    if (( CURRENT >= 1 )); then cur=\"${words[$CURRENT]:-}\"; fi\n");
            driver.append("    if [[ \"$cur\" == -* ]]; then state=\"args\"\n");
            driver.append("    elif (( CURRENT == 2 )); then state=\"cmd\"\n");
            driver.append("    else state=\"args\"\n");
            driver.append("    fi\n");
            driver.append("    if [[ \" $* \" == *\" -C \"* ]]; then\n");
            driver.append("        words=(\"${words[@]:1}\")\n");
            driver.append("        (( CURRENT-- ))\n");
            driver.append("    fi\n");
            driver.append("}\n");
            driver.append("_describe() {\n");
            driver.append("    local -a vals\n");
            driver.append("    eval \"vals=(\\\"\\${$2[@]}\\\")\"\n");
            driver.append("    local v\n");
            driver.append("    for v in \"${vals[@]}\"; do\n");
            driver.append("        print -r \"DESCRIBE: $v\" >> \"$CAPTURE\"\n");
            driver.append("    done\n");
            driver.append("}\n");
            driver.append("_files() {\n");
            driver.append("    print -r \"FILES\" >> \"$CAPTURE\"\n");
            driver.append("}\n");
            driver.append("words=(");
            for (String word : words)
                driver.append("\"").append(word.replace("\"", "\\\"")).append("\" ");
            driver.append(")\n");
            driver.append("CURRENT=").append(current).append("\n");
            driver.append("source \"").append(scriptFile.getAbsolutePath()).append("\"\n");
            File driverFile = new File(dir, "driver.zsh");
            Files.write(driverFile.toPath(),
                    driver.toString().getBytes(StandardCharsets.UTF_8));

            Process process = new ProcessBuilder("zsh", "-f", driverFile.getAbsolutePath())
                    .redirectErrorStream(true).start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("zsh completion timed out");
            }
            String output = new String(readAll(process.getInputStream()),
                    StandardCharsets.UTF_8);
            if (process.exitValue() != 0)
                throw new IllegalStateException("zsh failed: " + output);
            if (!captureFile.isFile())
                throw new IllegalStateException("zsh produced no capture: " + output);
            return new String(Files.readAllBytes(captureFile.toPath()), StandardCharsets.UTF_8);
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
        // The exact #670 repro: words=(cmd mid leaf --leaf), CURRENT=4.
        String specs = terminalSpecs(completeInZsh(script(), 4, "cmd", "mid", "leaf", "--leaf"));

        assertTrue("Leaf option must complete at depth, got: " + specs,
                specs.contains("--leaf-only"));
    }

    @Test
    public void testRootOptionsCompleted() throws Exception {
        String specs = terminalSpecs(completeInZsh(script(), 2, "cmd", "--"));

        assertTrue("Root options must complete, got: " + specs,
                specs.contains("--global"));
    }

    @Test
    public void testMidOptionsCompleted() throws Exception {
        String specs = terminalSpecs(completeInZsh(script(), 3, "cmd", "mid", "--"));

        assertTrue("Mid options must complete, got: " + specs,
                specs.contains("--mid-opt"));
        assertTrue("Inherited options must complete at depth, got: " + specs,
                specs.contains("--global"));
    }

    @Test
    public void testLeafOptionsWithInheritedCompleted() throws Exception {
        String specs = terminalSpecs(completeInZsh(script(), 4, "cmd", "mid", "leaf", "--"));

        assertTrue("Leaf options must complete, got: " + specs,
                specs.contains("--leaf-only"));
        assertTrue("Inherited options must complete at the leaf, got: " + specs,
                specs.contains("--global"));
    }

    @Test
    public void testLeafSubcommandCompleted() throws Exception {
        String captured = completeInZsh(script(), 3, "cmd", "mid", "");

        assertTrue("Leaf subcommand must complete, got: " + captured,
                captured.contains("DESCRIBE: leaf:leaf"));
    }

    @Test
    public void testAliasCompletedAtDepth() throws Exception {
        String specs = terminalSpecs(completeInZsh(script(), 4, "cmd", "mid", "leaf", "--l"));

        assertTrue("Alias must complete at depth, got: " + specs,
                specs.contains("--lo"));
        assertTrue("Primary must complete at depth, got: " + specs,
                specs.contains("--leaf-only"));
    }

    @Test
    public void testOptionValuesCompletedAtDepth() throws Exception {
        String specs = terminalSpecs(completeInZsh(script(), 5, "cmd", "mid", "leaf", "--mode", ""));

        assertTrue("Allowed values must complete at depth, got: " + specs,
                specs.contains("fast"));
        assertTrue("Allowed values must complete at depth, got: " + specs,
                specs.contains("slow"));
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

        Set<String> staticCandidates = longOptionNames(
                terminalSpecs(completeInZsh(script(), 4, "cmd", "mid", "leaf", "--")));

        assertEquals("Static and dynamic completion must agree at depth",
                dynamic, staticCandidates);
    }

    /**
     * The specs from the terminal {@code _arguments} call. The dispatch chain
     * is linear (each level calls {@code _arguments} once, then dispatches at
     * most once), and the real {@code _arguments} returns early on the
     * {@code ->state} action without offering intermediate options
     * (verified interactively: {@code --mid-opt} is not listed at the leaf),
     * so the last call's specs are the offered set.
     */
    private static String terminalSpecs(String captured) {
        String last = "";
        for (String line : captured.split("\\r?\\n")) {
            if (line.startsWith("ARG-SPECS:"))
                last = line;
        }
        return last;
    }

    private static final Pattern LONG_OPTION = Pattern.compile("--([a-zA-Z][a-zA-Z0-9-]*)");

    private static Set<String> longOptionNames(String captured) {
        Set<String> names = new HashSet<>();
        for (String line : captured.split("\\r?\\n")) {
            if (!line.startsWith("ARG-SPECS:"))
                continue;
            Matcher matcher = LONG_OPTION.matcher(line);
            while (matcher.find())
                names.add("--" + matcher.group(1));
        }
        return names;
    }
}
