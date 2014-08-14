/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.eof;

import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.aesh.AeshCommandCompletionTest;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.TestTerminal;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class IgnoreEofTest extends BaseConsoleTest {

    @Test
    public void ignoreeofDefaultVi() throws Exception {
        SettingsBuilder builder = new SettingsBuilder();
        builder.enableAlias(true);
        builder.persistAlias(false);
        builder.mode(Mode.VI);

        invokeTestConsole(1, new Setup() {
            @Override
            public void call(Console console, OutputStream out) throws Exception {
                console.getExportManager().addVariable("export ignoreeof = 2");

                String BUF = "asdfasdf";

                out.write(BUF.getBytes());
                out.flush();
                Thread.sleep(100);

                assertEquals(BUF, console.getBuffer());

                out.write(Key.CTRL_D.getFirstValue());
                out.flush();
                Thread.sleep(100);

                assertEquals("", console.getBuffer());

                out.write(Key.CTRL_D.getFirstValue());
                out.flush();
                Thread.sleep(100);

                assertTrue(console.isRunning());

                out.write(Key.CTRL_D.getFirstValue());
                out.flush();
                Thread.sleep(100);

                out.write(Key.CTRL_D.getFirstValue());
                out.flush();
                Thread.sleep(100);

                 assertFalse(console.isRunning());
            }
        }, new Verify() {
           @Override
           public int call(Console console, ConsoleOperation op) {
               return 0;
           }
        }, builder);
    }

    @Test
    public void ignoreeofDefaultEmacs() throws Exception {
        SettingsBuilder builder = new SettingsBuilder();
        builder.enableAlias(true);
        builder.persistAlias(false);
        builder.mode(Mode.EMACS);

        invokeTestConsole(1, new Setup() {
            @Override
            public void call(Console console, OutputStream out) throws Exception {
                console.getExportManager().addVariable("export ignoreeof = 1");

                String BUF = "a";

                out.write(BUF.getBytes());
                Thread.sleep(100);

                assertEquals(BUF, console.getBuffer());

                out.write(Key.CTRL_D.getFirstValue());
                out.flush();
                Thread.sleep(100);

                assertTrue(console.isRunning());

                out.write(Key.ENTER.getFirstValue());
                out.flush();
                Thread.sleep(100);

                out.write(Key.CTRL_D.getFirstValue());
                out.flush();
                Thread.sleep(100);

                assertTrue(console.isRunning());

                out.write(Key.CTRL_D.getFirstValue());
                out.flush();
                Thread.sleep(100);

                assertFalse(console.isRunning());
            }
        }, new Verify() {
           @Override
           public int call(Console console, ConsoleOperation op) {
               return 0;
           }
        }, builder);
    }


    @Test
    public void resetEOF() throws Exception {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .setPersistExport(false)
                .logging(true)
                .enableExport(true)
                .mode(Mode.VI)
                .readAhead(true)
                .create();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(AeshCommandCompletionTest.FooCommand.class)
                .create();

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry)
                .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();
        aeshConsole.start();

        outputStream.write(("export ignoreeof = 2").getBytes());
        outputStream.flush();
        Thread.sleep(100);

        outputStream.write(Key.CTRL_D.getFirstValue());
        outputStream.flush();
        Thread.sleep(100);

        outputStream.write(Key.CTRL_D.getFirstValue());
        outputStream.flush();
        Thread.sleep(100);

        outputStream.write(("foo").getBytes());
        outputStream.flush();
        Thread.sleep(100);

        outputStream.write(Key.CTRL_D.getFirstValue());
        outputStream.flush();
        Thread.sleep(100);

        outputStream.write(Key.CTRL_D.getFirstValue());
        outputStream.flush();
        Thread.sleep(100);

        outputStream.write(Key.CTRL_D.getFirstValue());
        outputStream.flush();
        Thread.sleep(100);

        outputStream.write(Key.CTRL_D.getFirstValue());
        outputStream.flush();
        Thread.sleep(100);

        assertFalse(aeshConsole.isRunning());

    }
}
