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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
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
import org.junit.Test;

public class SyncAsyncParityTest {

    @CommandDefinition(name = "ok", description = "always succeeds")
    public static class OkCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            commandInvocation.println("ok");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "nope", description = "always fails")
    public static class NopeCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            commandInvocation.println("nope");
            return CommandResult.FAILURE;
        }
    }

    @CommandDefinition(name = "upper", description = "uppercases stdin")
    public static class UpperCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            try {
                java.io.InputStream stdin = commandInvocation.getStdin();
                if (stdin != null) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(stdin));
                    String line;
                    while ((line = reader.readLine()) != null)
                        commandInvocation.println(line.toUpperCase());
                }
            } catch (Exception e) {
                return CommandResult.FAILURE;
            }
            return CommandResult.SUCCESS;
        }
    }

    static class Run {
        final List<CommandResult> results = new ArrayList<>();
        String output;
    }

    private Run runScript(String script, boolean interactive) throws Exception {
        TestConnection connection = interactive
                ? new TestConnection()
                : TestConnection.nonInteractive();
        String[] lines = script.split(";");
        // The listener fires per executed unit: line 1 runs ok, nope, ok;
        // line 2 runs the pipeline terminal stage once
        CountDownLatch latch = new CountDownLatch(4);
        Run run = new Run();

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(OkCommand.class)
                .command(NopeCommand.class)
                .command(UpperCommand.class)
                .create();

        File historyFile = File.createTempFile("aesh-parity-history", ".txt");
        historyFile.deleteOnExit();

        Settings<CommandInvocation> settings = SettingsBuilder.<CommandInvocation> builder()
                .connection(connection)
                .enableOperatorParser(true)
                .commandRegistry(registry)
                .historyFile(historyFile)
                .commandExecutionListener((line, result, durationMs) -> {
                    run.results.add(result);
                    latch.countDown();
                })
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        for (String part : lines)
            connection.read(part.trim() + Config.getLineSeparator());
        assertTrue("Script should complete", latch.await(15, TimeUnit.SECONDS));
        run.output = connection.getOutputBuffer();
        console.stop();
        return run;
    }

    @Test
    public void testMixedOperatorsParity() throws Exception {
        String script = "ok && nope || ok; ok | upper";
        Run async = runScript(script, true);
        Run sync = runScript(script, false);

        assertEquals(async.results, sync.results);
        assertEquals(async.output, sync.output);
        // ok, nope, ok, then the pipeline terminal: per-unit listener fires
        assertEquals(4, async.results.size());
        assertEquals(CommandResult.SUCCESS, async.results.get(0));
        assertEquals(CommandResult.FAILURE, async.results.get(1));
        assertEquals(CommandResult.SUCCESS, async.results.get(2));
        assertEquals(CommandResult.SUCCESS, async.results.get(3));
        // && ran the next command on success; the pipe uppercased its input
        assertTrue(async.output.contains("nope"));
        assertTrue(async.output.contains("OK"));
    }
}
