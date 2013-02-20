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
import org.jboss.aesh.console.ConsoleOutput;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.terminal.TestTerminal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

        Settings settings = Settings.getInstance();
        settings.setReadInputrc(false);
        settings.setTerminal(new TestTerminal());
        settings.setInputStream(new ByteArrayInputStream(buffer.getBytes()));
        settings.setStdOut(new ByteArrayOutputStream());
        settings.setEditMode(Mode.EMACS);
        settings.resetEditMode();
        settings.setReadAhead(false);
        if(!Config.isOSPOSIXCompatible())
            settings.setAnsiConsole(false);
        Console console = Console.getInstance();
        console.reset();
        final StringBuilder in = new StringBuilder();
        String tmpString = null;
        console.setConsoleCallback(new ConsoleCallback() {
            @Override
            public int readConsoleOutput(ConsoleOutput output) throws IOException {
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

        /*
        while (true) {
            ConsoleOutput tmp = console.read(new Prompt(""));
            if(tmp != null) {
            }
            else
                break;
        }
        try {
            console.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(lastOnly) {
            assertEquals(expected, tmpString);
        }
        else
            assertEquals(expected, in.toString());
        */
    }

    public void assertEqualsViMode(final String expected, TestBuffer buffer) throws IOException {

        Settings settings = Settings.getInstance();
        settings.setReadInputrc(false);
        settings.setTerminal(new TestTerminal());
        settings.setInputStream(new ByteArrayInputStream(buffer.getBytes()));
        settings.setStdOut(new ByteArrayOutputStream());
        settings.setReadAhead(false);
        settings.setEditMode(Mode.VI);
        settings.resetEditMode();
        if(!Config.isOSPOSIXCompatible())
            settings.setAnsiConsole(false);

        Console console = Console.getInstance();
        console.reset();
        console.setConsoleCallback(new ConsoleCallback() {
            @Override
            public int readConsoleOutput(ConsoleOutput output) throws IOException {
                assertEquals(expected, output.getBuffer());
                return 0;
            }
        });

        console.start();
        try { Thread.sleep(100); }
        catch (InterruptedException e) { }
        console.stop();

        /*
        String in = null;
        while (true) {
            ConsoleOutput tmp = console.read("");
            if(tmp != null)
                in = tmp.getBuffer();
            else
                break;
        }
        try {
            console.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertEquals(expected, in);
        */
    }
}
