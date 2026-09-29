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
package org.aesh.command.impl.operator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.EnumSet;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.Execution;
import org.aesh.command.Executor;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.operator.OperatorType;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.console.AeshContext;
import org.aesh.console.DefaultAeshContext;
import org.aesh.io.FileResource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Input redirection resolves relative paths against the aesh working
 * directory (like output redirection) and reports missing files instead
 * of silently running with absent stdin (#653).
 */
public class InputRedirectionTest {

    @Rule
    public final TemporaryFolder tempDir = new TemporaryFolder();

    @CommandDefinition(name = "reader", description = "reads stdin")
    public static class ReaderCommand implements Command<CommandInvocation> {
        static volatile String seen;
        static volatile boolean nullStdin;
        static volatile boolean executed;

        static void reset() {
            seen = null;
            nullStdin = false;
            executed = false;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            executed = true;
            try {
                java.io.InputStream stdin = commandInvocation.getStdin();
                if (stdin == null) {
                    nullStdin = true;
                    return CommandResult.SUCCESS;
                }
                StringBuilder text = new StringBuilder();
                byte[] buffer = new byte[512];
                int n;
                while ((n = stdin.read(buffer)) != -1)
                    text.append(new String(buffer, 0, n, StandardCharsets.UTF_8));
                seen = text.toString();
            } catch (Exception e) {
                return CommandResult.FAILURE;
            }
            return CommandResult.SUCCESS;
        }
    }

    private CommandRuntime<CommandInvocation> buildRuntime(AeshContext context) throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder
                .<CommandInvocation> builder()
                .command(ReaderCommand.class)
                .create();
        return AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(registry)
                .aeshContext(context)
                .operators(EnumSet.allOf(OperatorType.class))
                .build();
    }

    private static File write(File dir, String name, String content) throws IOException {
        File file = new File(dir, name);
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    @Test
    public void testRelativeInputResolvedAgainstAeshCwd() throws Exception {
        write(tempDir.getRoot(), "a1.txt", "hello-a1");
        // The JVM cwd (surefire module dir) is elsewhere by construction.
        assertFalse(new File("a1.txt").exists());
        AeshContext context = new DefaultAeshContext(new FileResource(tempDir.getRoot()));
        ReaderCommand.reset();

        CommandResult result = buildRuntime(context).executeCommand("reader < a1.txt");

        assertEquals(CommandResult.SUCCESS, result);
        assertTrue("command must run", ReaderCommand.executed);
        assertFalse("stdin must be present", ReaderCommand.nullStdin);
        assertEquals("hello-a1", ReaderCommand.seen);
    }

    @Test
    public void testAbsoluteInputStillWorks() throws Exception {
        File file = write(tempDir.getRoot(), "a1.txt", "hello-abs");
        AeshContext context = new DefaultAeshContext(new FileResource(tempDir.getRoot()));
        ReaderCommand.reset();

        CommandResult result = buildRuntime(context)
                .executeCommand("reader < " + file.getAbsolutePath());

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("hello-abs", ReaderCommand.seen);
    }

    @Test
    public void testFilenameWithSpaces() throws Exception {
        write(tempDir.getRoot(), "my file.txt", "spaced");
        AeshContext context = new DefaultAeshContext(new FileResource(tempDir.getRoot()));
        ReaderCommand.reset();

        CommandResult result = buildRuntime(context).executeCommand("reader < \"my file.txt\"");

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("spaced", ReaderCommand.seen);
    }

    @Test
    public void testMissingFileFailsWithoutExecuting() throws Exception {
        AeshContext context = new DefaultAeshContext(new FileResource(tempDir.getRoot()));
        ReaderCommand.reset();

        try {
            buildRuntime(context).executeCommand("reader < missing.txt");
            fail("Expected IOException for missing redirected input");
        } catch (IOException e) {
            assertTrue("Error must name the file, got: " + e.getMessage(),
                    e.getMessage().contains("missing.txt"));
        }
        assertFalse("command must not run with absent stdin", ReaderCommand.executed);
    }

    @Test
    public void testDelegateStreamIsCachedAndCloseable() throws Exception {
        write(tempDir.getRoot(), "a1.txt", "cached");
        AeshContext context = new DefaultAeshContext(new FileResource(tempDir.getRoot()));
        CommandRuntime<CommandInvocation> runtime = buildRuntime(context);

        Executor<CommandInvocation> executor = runtime.buildExecutor("reader < a1.txt");
        assertEquals(1, executor.getExecutions().size());
        Execution<CommandInvocation> execution = executor.getExecutions().get(0);
        InputDelegate delegate = execution.getCommandInvocation().getConfiguration()
                .getInputRedirection();

        assertSame("reads share the cached stream", delegate.read(), delegate.read());
        delegate.close();
        assertNotNull("close releases; a later read reopens", delegate.read());
        delegate.close();
    }
}
