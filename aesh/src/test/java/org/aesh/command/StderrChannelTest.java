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
package org.aesh.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.StringWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.aesh.command.impl.invocation.DefaultCommandInvocation;
import org.aesh.command.impl.operator.OutputDelegate;
import org.aesh.command.impl.operator.PipeOperator;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.AeshContext;
import org.aesh.console.ReadlineConsole;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

public class StderrChannelTest {

    @CommandDefinition(name = "errout", description = "writes to stderr")
    public static class ErrCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            commandInvocation.printErr("oops");
            return CommandResult.SUCCESS;
        }
    }

    static class CapturingDelegate extends OutputDelegate {
        final StringWriter captured = new StringWriter();

        @Override
        protected BufferedWriter buildWriter() {
            return new BufferedWriter(captured);
        }
    }

    private static AeshContext context() {
        return SettingsBuilder.builder().build().aeshContext();
    }

    @Test
    public void testPrintErrDefaultsToTerminal() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(ErrCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.<CommandInvocation> builder()
                .connection(connection)
                .commandRegistry(registry)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        connection.clearOutputBuffer();
        connection.read("errout" + Config.getLineSeparator());
        String output = connection.waitForOutputContaining("oops", 5000);

        assertTrue(output.contains("oops"));
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        console.stop();
    }

    @Test
    public void testPrintErrUsesErrorRedirection() throws Exception {
        CapturingDelegate errors = new CapturingDelegate();
        CommandInvocationConfiguration config = new CommandInvocationConfiguration(context());
        config.setErrorRedirection(errors);
        assertTrue(config.hasErrorRedirection());

        DefaultCommandInvocation invocation = new DefaultCommandInvocation(null, config, null, null);
        invocation.printErr("oops");
        errors.close();

        assertTrue(errors.captured.toString().contains("oops"));
    }

    @Test
    public void testNoErrorRedirectionByDefault() {
        CommandInvocationConfiguration config = new CommandInvocationConfiguration(context());
        assertFalse(config.hasErrorRedirection());
        assertNull(config.getErrorRedirection());
    }

    @Test
    public void testPipeErrorChannelIsolated() throws Exception {
        PipeOperator pipe = new PipeOperator(context());

        assertFalse(pipe.hasErrorData());
        assertNull(pipe.getErrorData());

        pipe.getErrorDelegate().write("boom");
        pipe.getErrorDelegate().close();
        assertTrue(pipe.hasErrorData());

        BufferedInputStream err = pipe.getErrorData();
        byte[] buf = new byte[64];
        StringBuilder errText = new StringBuilder();
        int n;
        while ((n = err.read(buf)) != -1)
            errText.append(new String(buf, 0, n));
        assertEquals("boom", errText.toString());
        assertFalse(pipe.hasErrorData());

        BufferedInputStream out = pipe.getData();
        assertEquals(0, out.available());
    }

    @Test
    public void testPipeChannelsDoNotMix() throws Exception {
        PipeOperator pipe = new PipeOperator(context());

        pipe.getConfiguration().getOutputRedirection().write("out-text");
        pipe.getErrorDelegate().write("err-text");
        pipe.getConfiguration().getOutputRedirection().close();
        pipe.getErrorDelegate().close();

        BufferedInputStream err = pipe.getErrorData();
        byte[] buf = new byte[64];
        StringBuilder errText = new StringBuilder();
        int n;
        while ((n = err.read(buf)) != -1)
            errText.append(new String(buf, 0, n));
        assertEquals("err-text", errText.toString());

        BufferedInputStream out = pipe.getData();
        StringBuilder outText = new StringBuilder();
        while ((n = out.read(buf)) != -1)
            outText.append(new String(buf, 0, n));
        assertEquals("out-text", outText.toString());
    }
}
