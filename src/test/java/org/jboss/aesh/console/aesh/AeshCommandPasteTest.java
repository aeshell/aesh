/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.aesh;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.AeshConsoleImpl;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.terminal.TestTerminal;
import org.junit.Test;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandPasteTest {

    private static final String LINE_SEPARATOR = Config.getLineSeparator();

    @Test
    public void testPaste() throws IOException, InterruptedException {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
            .terminal(new TestTerminal())
            .inputStream(pipedInputStream)
            .outputStream(new PrintStream(byteArrayOutputStream))
            .setPersistExport(false)
            .logging(true)
            .create();

        CommandRegistry registry = new AeshCommandRegistryBuilder().create();

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
            .settings(settings)
            .commandRegistry(registry)
            .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();
        aeshConsole.start();

        outputStream.write(("FOO" + LINE_SEPARATOR + "FUU" + LINE_SEPARATOR + "bar").getBytes());
        outputStream.flush();
        Thread.sleep(100);
        assertEquals("bar", ((AeshConsoleImpl) aeshConsole).getBuffer());

        aeshConsole.stop();
    }

    @Test
    public void testPasteWhileACommandIsRunning() throws IOException, InterruptedException {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
            .terminal(new TestTerminal())
            .inputStream(pipedInputStream)
            .outputStream(new PrintStream(byteArrayOutputStream))
            .setPersistExport(false)
            .logging(true)
            .create();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
            .command(FooCommand.class)
            .create();

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
            .settings(settings)
            .commandRegistry(registry)
            .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();
        aeshConsole.start();

        outputStream.write(("foo" + LINE_SEPARATOR + "FUU" + LINE_SEPARATOR + "bar").getBytes());
        outputStream.flush();
        Thread.sleep(200);
        assertEquals("bar", ((AeshConsoleImpl) aeshConsole).getBuffer());

        aeshConsole.stop();
    }

    @CommandDefinition(name = "foo", description = "")
    private static class FooCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            return CommandResult.SUCCESS;
        }
    }

}
