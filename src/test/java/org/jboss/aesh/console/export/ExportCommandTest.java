/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.export;

import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.AeshConsoleImpl;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.KeyOperation;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.TestTerminal;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ExportCommandTest {

    private KeyOperation completeChar =  new KeyOperation(Key.CTRL_I, Operation.COMPLETE);
    private KeyOperation backSpace =  new KeyOperation(Key.BACKSPACE, Operation.DELETE_PREV_CHAR);

    @Test
    public void testExportCompletionAndCommand() throws IOException, InterruptedException {

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

        outputStream.write(("exp").getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();
        Thread.sleep(100);
        assertEquals("export ", ((AeshConsoleImpl) aeshConsole).getBuffer());


        outputStream.write(("FOO=/tmp"+Config.getLineSeparator()).getBytes());
        outputStream.write(("export"+Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(100);
        assertTrue(byteArrayOutputStream.toString().contains("FOO=/tmp"));

        outputStream.write(("export BAR=$F").getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();
        Thread.sleep(100);
        assertEquals("export BAR=$FOO ", ((AeshConsoleImpl) aeshConsole).getBuffer());

        outputStream.write(backSpace.getFirstValue());
        outputStream.write((":/opt"+Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(100);
        outputStream.write(("export"+Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(400);
        assertTrue(byteArrayOutputStream.toString().contains("BAR=/tmp:/opt"));

        outputStream.write(("$").getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();
        Thread.sleep(400);
        assertTrue(byteArrayOutputStream.toString().contains("$FOO"));
        assertTrue(byteArrayOutputStream.toString().contains("$BAR"));

        outputStream.write(("B").getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();
        Thread.sleep(400);
        assertEquals("$BAR ", ((AeshConsoleImpl) aeshConsole).getBuffer());

        outputStream.write(Config.getLineSeparator().getBytes());
        outputStream.flush();
        Thread.sleep(400);

        assertTrue(byteArrayOutputStream.toString().contains("/tmp:/opt"));

        aeshConsole.stop();
    }

}
