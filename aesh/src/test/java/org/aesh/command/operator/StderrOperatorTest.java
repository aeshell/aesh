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
package org.aesh.command.operator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.ReadlineConsole;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class StderrOperatorTest {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @CommandDefinition(name = "both", description = "writes to both streams")
    public static class BothCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            commandInvocation.println("out-text");
            commandInvocation.printErr("err-text");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "cat", description = "echoes stdin")
    public static class CatCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            try {
                java.io.InputStream stdin = commandInvocation.getStdin();
                if (stdin != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stdin));
                    String line;
                    while ((line = reader.readLine()) != null)
                        commandInvocation.println(line);
                }
            } catch (Exception e) {
                return CommandResult.FAILURE;
            }
            return CommandResult.SUCCESS;
        }
    }

    private ReadlineConsole startConsole(TestConnection connection, CountDownLatch latch) throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(BothCommand.class)
                .command(CatCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.<CommandInvocation> builder()
                .logging(true)
                .connection(connection)
                .commandRegistry(registry)
                .historyFile(File.createTempFile("aesh-stderr-test-history", ".txt"))
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();
        return console;
    }

    @Test
    public void testErrorRedirection() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);
        ReadlineConsole console = startConsole(connection, latch);

        File err = new File(tempDir.getRoot(), "err.txt");
        connection.clearOutputBuffer();
        connection.read("both 2> " + err.getCanonicalPath() + Config.getLineSeparator());
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        console.stop();

        List<String> errLines = Files.readAllLines(err.toPath());
        assertEquals(1, errLines.size());
        assertEquals("err-text", errLines.get(0));
        assertTrue("stdout should stay on the terminal",
                connection.getOutputBuffer().contains("out-text"));
        assertFalse("stderr must not leak to the terminal",
                connection.getOutputBuffer().contains("err-text"));
    }

    @Test
    public void testErrorAppend() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(2);
        ReadlineConsole console = startConsole(connection, latch);

        File err = new File(tempDir.getRoot(), "err-append.txt");
        connection.read("both 2>> " + err.getCanonicalPath() + Config.getLineSeparator());
        connection.read("both 2>> " + err.getCanonicalPath() + Config.getLineSeparator());
        assertTrue("Commands should complete", latch.await(5, TimeUnit.SECONDS));
        console.stop();

        List<String> errLines = Files.readAllLines(err.toPath());
        assertEquals(2, errLines.size());
        assertEquals("err-text", errLines.get(0));
        assertEquals("err-text", errLines.get(1));
    }

    @Test
    public void testPipeBothStreams() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);
        ReadlineConsole console = startConsole(connection, latch);

        connection.clearOutputBuffer();
        connection.read("both |& cat" + Config.getLineSeparator());
        assertTrue("Pipeline should complete", latch.await(5, TimeUnit.SECONDS));
        console.stop();

        String output = connection.getOutputBuffer();
        assertTrue("stdout line should flow through the pipe, got: " + output,
                output.contains("out-text"));
        assertTrue("stderr line should flow through the pipe, got: " + output,
                output.contains("err-text"));
    }

    @Test
    public void testMergeErrorIntoOutputFile() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);
        ReadlineConsole console = startConsole(connection, latch);

        File out = new File(tempDir.getRoot(), "merged.txt");
        connection.clearOutputBuffer();
        connection.read("both > " + out.getCanonicalPath() + " 2>&1" + Config.getLineSeparator());
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        console.stop();

        List<String> lines = Files.readAllLines(out.toPath());
        assertEquals(2, lines.size());
        assertTrue(lines.contains("out-text"));
        assertTrue(lines.contains("err-text"));
        assertFalse("nothing should reach the terminal",
                connection.getOutputBuffer().contains("out-text")
                        || connection.getOutputBuffer().contains("err-text"));
    }

    @Test
    public void testStackedOutputAndErrorRedirects() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);
        ReadlineConsole console = startConsole(connection, latch);

        File out = new File(tempDir.getRoot(), "out.txt");
        File err = new File(tempDir.getRoot(), "err.txt");
        connection.read("both > " + out.getCanonicalPath() + " 2> " + err.getCanonicalPath()
                + Config.getLineSeparator());
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        console.stop();

        List<String> outLines = Files.readAllLines(out.toPath());
        assertEquals(1, outLines.size());
        assertEquals("out-text", outLines.get(0));
        List<String> errLines = Files.readAllLines(err.toPath());
        assertEquals(1, errLines.size());
        assertEquals("err-text", errLines.get(0));
    }
}
