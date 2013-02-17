/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.AeshTestCase;
import org.jboss.aesh.TestBuffer;
import org.jboss.aesh.console.operator.ControlOperator;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.edit.KeyOperation;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.TestTerminal;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConsoleTest extends BaseConsoleTest {

    public ConsoleTest(String test) {
        super(test);
    }

    public void testMultiLine() throws IOException {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        Console console = getTestConsole(pipedInputStream);
        outputStream.write(("ls \\").getBytes());
        outputStream.write(("\n").getBytes());
        outputStream.write(("foo \\").getBytes());
        outputStream.write(("\n").getBytes());
        outputStream.write(("bar\n").getBytes());
        ConsoleOutput output = console.read(new Prompt(""));
        assertEquals("ls foo bar", output.getBuffer());
    }


}
