/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh;

import junit.framework.TestCase;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleCallback;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.terminal.TestTerminal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public abstract class AeshTestCase extends TestCase {

    public AeshTestCase(String test) {
        super(test);
    }

    public void assertEquals(String expected, TestBuffer buffer) throws IOException {
        assertEquals(expected, buffer, false);
    }

    public void assertEquals(final String expected, TestBuffer buffer, final boolean lastOnly) throws IOException {

        SettingsBuilder builder = new SettingsBuilder();
        builder.readInputrc(false);
        builder.terminal(new TestTerminal());
        builder.inputStream(new ByteArrayInputStream(buffer.getBytes()));
        builder.outputStream(new PrintStream(new ByteArrayOutputStream()));
        builder.readAhead(false);
        builder.mode(Mode.EMACS);
        if(!Config.isOSPOSIXCompatible())
            builder.ansi(false);

        Console console = new Console(builder.create());
        final StringBuilder in = new StringBuilder();
        String tmpString = null;
        console.setConsoleCallback(new ConsoleCallback() {
            @Override
            public int readConsoleOutput(ConsoleOperation output) {
                if(lastOnly) {
                    assertEquals(expected, output.getBuffer());
                }
                else
                    assertEquals(expected, output.getBuffer());
                return 0;
            }
        });
        console.start();
        try { Thread.sleep(100); }
        catch (InterruptedException e) { }
        console.stop();

    }

    public void assertEqualsViMode(final String expected, TestBuffer buffer) throws IOException {

        SettingsBuilder builder = new SettingsBuilder();
        builder.readInputrc(false);
        builder.terminal(new TestTerminal());
        builder.inputStream(new ByteArrayInputStream(buffer.getBytes()));
        builder.outputStream(new PrintStream(new ByteArrayOutputStream()));
        builder.readAhead(false);
        builder.mode(Mode.VI);
        if(!Config.isOSPOSIXCompatible())
            builder.ansi(false);

        Console console = new Console(builder.create());
        console.setConsoleCallback(new ConsoleCallback() {
            @Override
            public int readConsoleOutput(ConsoleOperation output) {
                assertEquals(expected, output.getBuffer());
                return 0;
            }
        });

        console.start();
        try { Thread.sleep(100); }
        catch (InterruptedException e) { }
        console.stop();
    }

}
