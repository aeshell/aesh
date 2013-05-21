/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.terminal;

import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleCallback;
import org.jboss.aesh.console.ConsoleOutput;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.terminal.CharacterType;
import org.jboss.aesh.terminal.Color;
import org.jboss.aesh.terminal.TerminalString;
import org.junit.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TerminalOutputTest extends BaseConsoleTest {


    @Test
    public void terminalString() throws IOException, InterruptedException {

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        Console console = getTestConsole(pipedInputStream);
        console.setPrompt(new Prompt(new TerminalString("[test]", Color.BLACK_BG, Color.WHITE_TEXT, CharacterType.PLAIN)));
        console.setConsoleCallback(new ConsoleCallback() {
            @Override
            public int readConsoleOutput(ConsoleOutput output) throws IOException {
                assertEquals("FOO", output.getBuffer());
                return 0;
            }
        });

        console.start();
        outputStream.write(new TerminalString("FOO", CharacterType.BOLD).getCharacters().getBytes());
        outputStream.write("\n".getBytes());
        Thread.sleep(100);

        console.stop();
    }
}
