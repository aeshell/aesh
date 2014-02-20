/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.eof;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.jboss.aesh.console.AeshConsoleCallback;
import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.terminal.Key;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class IgnoreEofTest extends BaseConsoleTest {

    @Test
    public void ignoreeofDefaultVi() throws IOException, InterruptedException {
        SettingsBuilder builder = new SettingsBuilder();
        builder.enableAlias(true);
        builder.persistAlias(false);
        builder.mode(Mode.VI);

        PipedOutputStream stdin = new PipedOutputStream();
        Console console = getTestConsole(builder, new PipedInputStream(stdin));
        console.setConsoleCallback(new AeshConsoleCallback() {
            @Override
            public int execute(ConsoleOperation output) {
                return 0;
            }
        });
        console.start();
        console.getExportManager().addVariable("export ignoreeof = 2");

        String BUF = "asdfasdf";

        stdin.write(BUF.getBytes());
        stdin.flush();
        Thread.sleep(100);

        assertEquals(BUF, console.getBuffer());

        stdin.write(Key.CTRL_D.getFirstValue());
        stdin.flush();
        Thread.sleep(100);

        assertEquals("", console.getBuffer());

        stdin.write(Key.CTRL_D.getFirstValue());
        stdin.flush();
        Thread.sleep(100);

        assertTrue(console.isRunning());

        stdin.write(Key.CTRL_D.getFirstValue());
        stdin.flush();
        Thread.sleep(100);

        assertTrue(console.isRunning());

        stdin.write(Key.CTRL_D.getFirstValue());
        stdin.flush();
        Thread.sleep(100);

        assertFalse(console.isRunning());
    }

    @Test
    public void ignoreeofDefaultEmacs() throws IOException, InterruptedException {
        SettingsBuilder builder = new SettingsBuilder();
        builder.enableAlias(true);
        builder.persistAlias(false);
        builder.mode(Mode.EMACS);

        PipedOutputStream stdin = new PipedOutputStream();
        Console console = getTestConsole(builder, new PipedInputStream(stdin));
        console.setConsoleCallback(new AeshConsoleCallback() {
            @Override
            public int execute(ConsoleOperation output) {
                return 0;
            }
        });
        console.start();
        console.getExportManager().addVariable("export ignoreeof = 1");

        String BUF = "a";

        stdin.write(BUF.getBytes());
        Thread.sleep(100);

        assertEquals(BUF, console.getBuffer());

        stdin.write(Key.CTRL_D.getFirstValue());
        stdin.flush();
        Thread.sleep(100);

        assertTrue(console.isRunning());

        stdin.write(Key.CTRL_D.getFirstValue());
        stdin.flush();
        Thread.sleep(100);

        assertTrue(console.isRunning());

        stdin.write(Key.CTRL_D.getFirstValue());
        stdin.flush();
        Thread.sleep(100);

        assertFalse(console.isRunning());

    }

}
