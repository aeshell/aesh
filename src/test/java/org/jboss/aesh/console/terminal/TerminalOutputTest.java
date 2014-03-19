/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.terminal;

import static org.junit.Assert.assertEquals;

import java.io.OutputStream;

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

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TerminalOutputTest extends BaseConsoleTest {


    @Test
    public void terminalString() throws Exception {
        invokeTestConsole(new Setup() {
            @Override
            public void call(Console console, OutputStream out) throws Exception {
                console.setPrompt(new Prompt(new TerminalString("[test]", new TerminalColor(Color.WHITE, Color.BLACK),
                        new TerminalTextStyle(CharacterType.FAINT))));
                out.write(new TerminalString("FOO", new TerminalTextStyle( CharacterType.BOLD)).getCharacters().getBytes());
                out.write(Config.getLineSeparator().getBytes());
            }
        }, new Verify() {
           @Override
           public int call(Console console, ConsoleOperation op) {
               assertEquals("FOO", op.getBuffer());
               return 0;
           }
        });
    }
}
