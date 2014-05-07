/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.eof;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;

import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.terminal.Key;
import org.junit.Test;

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

                assertFalse(console.isRunning());            }
        }, new Verify() {
           @Override
           public int call(Console console, ConsoleOperation op) {
               return 0;
           }
        }, builder);
    }
}
