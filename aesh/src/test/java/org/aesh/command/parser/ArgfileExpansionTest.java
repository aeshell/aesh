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
package org.aesh.command.parser;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.aesh.AeshRuntimeRunner;
import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.impl.parser.ArgfileExpander;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.junit.Test;

/**
 * {@code @file} arguments expand into the arguments listed in that file,
 * opt-in via the runtime builder or the runner (#772).
 */
public class ArgfileExpansionTest {

    @Test
    public void testNullAndPlainArgsPassThrough() throws Exception {
        assertNull(ArgfileExpander.expand(null));
        String[] plain = { "ps", "--sort" };
        assertSame("no @ words costs nothing", plain, ArgfileExpander.expand(plain));
    }

    @Test
    public void testBasicExpansion() throws Exception {
        Path file = writeTempFile("--sort\n--json\nfoo\n");
        try {
            assertArrayEquals(new String[] { "ps", "--sort", "--json", "foo" },
                    ArgfileExpander.expand(new String[] { "ps", "@" + file }));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void testQuotesGroupWords() throws Exception {
        Path file = writeTempFile("--msg \"hello world\" 'a  b'\nC:\\temp\\x.txt\n");
        try {
            assertArrayEquals(new String[] { "--msg", "hello world", "a  b", "C:\\temp\\x.txt" },
                    ArgfileExpander.expand(new String[] { "@" + file }));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void testCommentLinesDroppedInlineKept() throws Exception {
        Path file = writeTempFile("# full-line comment\n   # indented comment\n--sort # trailing\n");
        try {
            assertArrayEquals(new String[] { "--sort", "#", "trailing" },
                    ArgfileExpander.expand(new String[] { "@" + file }));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void testEscapeAndUntouchedForms() throws Exception {
        Path file = writeTempFile("value\n");
        try {
            String at = "@" + file;
            assertArrayEquals(new String[] { "@" + file, "--body=" + at, "@", "plain" },
                    ArgfileExpander.expand(new String[] { "@@" + file, "--body=" + at, "@", "plain" }));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void testEmptyFileExpandsToNothing() throws Exception {
        Path file = writeTempFile("");
        try {
            assertArrayEquals(new String[] { "ps" },
                    ArgfileExpander.expand(new String[] { "ps", "@" + file }));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void testMissingFileIsUsageError() {
        try {
            ArgfileExpander.expand(new String[] { "@no-such-argfile-xyz.txt" });
            fail("Expected missing file to fail");
        } catch (CommandLineParserException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Argument file not found"));
        }
    }

    @Test
    public void testNestedFiles() throws Exception {
        Path inner = writeTempFile("--json\n");
        Path outer = writeTempFile("--sort\n@" + inner + "\n");
        try {
            assertArrayEquals(new String[] { "--sort", "--json" },
                    ArgfileExpander.expand(new String[] { "@" + outer }));
        } finally {
            Files.deleteIfExists(inner);
            Files.deleteIfExists(outer);
        }
    }

    @Test
    public void testCircularReferenceIsUsageError() throws Exception {
        Path first = Files.createTempFile("argfile-a", ".txt");
        Path second = Files.createTempFile("argfile-b", ".txt");
        try {
            Files.write(first, ("@" + second + "\n").getBytes(StandardCharsets.UTF_8));
            Files.write(second, ("@" + first + "\n").getBytes(StandardCharsets.UTF_8));
            try {
                ArgfileExpander.expand(new String[] { "@" + first });
                fail("Expected circular reference to fail");
            } catch (CommandLineParserException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("Circular"));
            }
        } finally {
            Files.deleteIfExists(first);
            Files.deleteIfExists(second);
        }
    }

    @Test
    public void testRuntimeExpandsWithFlag() throws Exception {
        Path file = writeTempFile("--sort\nname\nfoo\n");
        try {
            ArgfileTool.seenSort = null;
            ArgfileTool.seenTarget = null;
            CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder
                    .<CommandInvocation> builder()
                    .commandRegistry(AeshCommandRegistryBuilder
                            .<CommandInvocation> builder().command(ArgfileTool.class).create())
                    .argumentFiles(true)
                    .build();

            assertEquals(CommandResult.SUCCESS,
                    runtime.executeCommand("argtool", new String[] { "@" + file }));
            assertEquals("name", ArgfileTool.seenSort);
            assertEquals("foo", ArgfileTool.seenTarget);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void testRuntimeIgnoresWithoutFlag() throws Exception {
        Path file = writeTempFile("--sort\n");
        try {
            CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder
                    .<CommandInvocation> builder()
                    .commandRegistry(AeshCommandRegistryBuilder
                            .<CommandInvocation> builder().command(NoPosTool.class).create())
                    .build();

            try {
                runtime.executeCommand("nopos", new String[] { "@" + file });
                fail("Expected @file to stay a positional without the flag");
            } catch (CommandLineParserException e) {
                assertEquals("Unknown argument '@" + file + "': nopos takes no arguments", e.getMessage());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void testRunnerExpandsWithFlag() throws Exception {
        Path file = writeTempFile("--sort\nname\nfoo\n");
        try {
            ArgfileTool.seenSort = null;
            ArgfileTool.seenTarget = null;
            assertEquals(CommandResult.SUCCESS, AeshRuntimeRunner.builder()
                    .command(ArgfileTool.class)
                    .argumentFiles(true)
                    .args("@" + file)
                    .execute());
            assertEquals("name", ArgfileTool.seenSort);
            assertEquals("foo", ArgfileTool.seenTarget);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void testRunnerIgnoresWithoutFlag() throws Exception {
        // --sort x would succeed expanded; unexpanded it is a usage error.
        Path file = writeTempFile("--sort\nx\n");
        try {
            assertEquals(CommandResult.USAGE_ERROR, AeshRuntimeRunner.builder()
                    .command(NoPosTool.class)
                    .args("@" + file)
                    .execute());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @CommandDefinition(name = "nopos", description = "No positionals tool", generateHelp = true)
    public static class NoPosTool implements Command<CommandInvocation> {
        @Option(name = "sort", description = "Sort order")
        String sort;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    private static Path writeTempFile(String content) throws Exception {
        Path file = Files.createTempFile("argfile", ".txt");
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    @CommandDefinition(name = "argtool", description = "Argfile tool", generateHelp = true)
    public static class ArgfileTool implements Command<CommandInvocation> {
        static String seenSort;
        static String seenTarget;

        @Option(name = "sort", description = "Sort order")
        String sort;

        @Argument(description = "The target")
        String target;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            seenSort = sort;
            seenTarget = target;
            return CommandResult.SUCCESS;
        }
    }
}
