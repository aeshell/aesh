/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.terminal;

import org.jboss.aesh.console.AeshConsoleCallback;
import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.terminal.CharacterType;
import org.jboss.aesh.terminal.Color;
import org.jboss.aesh.terminal.TerminalColor;
import org.jboss.aesh.terminal.TerminalString;
import org.jboss.aesh.terminal.TerminalTextStyle;
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
        console.setPrompt(new Prompt(new TerminalString("[test]", new TerminalColor(Color.WHITE, Color.BLACK),
                new TerminalTextStyle(CharacterType.FAINT))));
        console.setConsoleCallback(new AeshConsoleCallback() {
            @Override
            public int execute(ConsoleOperation output) {
                assertEquals("FOO", output.getBuffer());
                return 0;
            }
        });

        console.start();
        outputStream.write(new TerminalString("FOO", new TerminalTextStyle( CharacterType.BOLD)).getCharacters().getBytes());
        outputStream.write(Config.getLineSeparator().getBytes());
        Thread.sleep(100);

        console.stop();
    }
}
