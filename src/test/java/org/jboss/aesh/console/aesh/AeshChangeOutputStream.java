/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.aesh;

import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.AeshConsoleImpl;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.terminal.TestTerminal;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshChangeOutputStream {

    private static final String LINE_SEPARATOR = Config.getLineSeparator();

    @Test
    public void changeOutputStream() throws IOException, InterruptedException {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .setPersistExport(false)
                .persistHistory(false)
                .logging(true)
                .ansi(false)
                .create();

        CommandRegistry registry = new AeshCommandRegistryBuilder().create();

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry)
                .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();
        aeshConsole.start();

        outputStream.write(("FOO" + LINE_SEPARATOR).getBytes());
        outputStream.flush();
        Thread.sleep(100);

        assertEquals("FOO"+LINE_SEPARATOR+"Command: FOO was not found."+Config.getLineSeparator(), byteArrayOutputStream.toString());

        ByteArrayOutputStream newByteArrayOutputStream = new ByteArrayOutputStream();
        ((AeshConsoleImpl) aeshConsole).changeOutputStream(new PrintStream(newByteArrayOutputStream));

        Thread.sleep(100);

        outputStream.write(("Foo" + LINE_SEPARATOR).getBytes());
        outputStream.flush();
        Thread.sleep(100);

        assertEquals("Foo"+LINE_SEPARATOR+"Command: Foo was not found."+Config.getLineSeparator(), newByteArrayOutputStream.toString());

        aeshConsole.stop();
    }


}
