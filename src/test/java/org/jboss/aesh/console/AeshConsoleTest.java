/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.edit.KeyOperation;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;
import org.junit.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshConsoleTest extends BaseConsoleTest {

    private KeyOperation completeChar =  new KeyOperation(Key.CTRL_I, Operation.COMPLETE);

    @Test
    public void testAeshConsole() throws IOException, InterruptedException {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        Settings settings = getDefaultSettings(pipedInputStream, null);

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder().settings(settings);
        consoleBuilder.command(FooTestCommand.class);

        AeshConsole aeshConsole = consoleBuilder.create();
        aeshConsole.start();

        outputStream.write(("foo ").getBytes());
        outputStream.write(completeChar.getFirstValue());

        outputStream.write("\n".getBytes());


    }

    @CommandDefinition(name="foo", description = "")
    public static class FooTestCommand implements Command {

        @InjectConsole
        AeshConsole console;

        @Option
        private String bar;

        @Override
        public void execute() throws IOException {
            console.out().println("FOO");
        }
    }

}
