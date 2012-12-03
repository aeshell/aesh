/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.alias;

import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOutput;
import org.jboss.aesh.console.settings.Settings;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConsoleAliasTest extends BaseConsoleTest {

    public ConsoleAliasTest(String test) {
        super(test);
    }

    public void testAlias() throws IOException {
        Settings.getInstance().setAliasFile(Config.isOSPOSIXCompatible() ?
                new File("src/test/resources/alias1") : new File("src\\test\\resources\\alias1"));
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        Console console = getTestConsole(pipedInputStream);
        outputStream.write("ll\n".getBytes());
        ConsoleOutput output = console.read(null);
        assertEquals("ls -alF", output.getBuffer());
        outputStream.write("grep -l\n".getBytes());
        output = console.read(null);
        assertEquals("grep --color=auto -l", output.getBuffer());

        console.stop();
    }

}
