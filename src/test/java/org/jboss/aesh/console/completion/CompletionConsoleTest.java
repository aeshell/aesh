/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.completion;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOutput;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.edit.KeyOperation;
import org.jboss.aesh.edit.actions.Operation;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CompletionConsoleTest extends BaseConsoleTest {

    private KeyOperation completeChar =  new KeyOperation(9, Operation.COMPLETE);

    public CompletionConsoleTest(String test) {
        super(test);
    }

    public void testCompletion() throws IOException {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        Console console = getTestConsole(pipedInputStream);

        Completion completion = new Completion() {
            @Override
            public void complete(CompleteOperation co) {
                if(co.getBuffer().equals("foo"))
                    co.addCompletionCandidate("foobar");
            }
        };
        console.addCompletion(completion);


        Completion completion2 = new Completion() {
            @Override
            public void complete(CompleteOperation co) {
                if(co.getBuffer().equals("bar")) {
                    co.addCompletionCandidate("barfoo");
                    co.doAppendSeparator(false);
                }
            }
        };
        console.addCompletion(completion2);

        Completion completion3 = new Completion() {
            @Override
            public void complete(CompleteOperation co) {
                if(co.getBuffer().equals("le")) {
                    co.addCompletionCandidate("less");
                    co.setSeparator(':');
                }
            }
        };
        console.addCompletion(completion3);

        outputStream.write("foo".getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.write("\n".getBytes());
        ConsoleOutput output = console.read(null);
        assertEquals("foobar ", output.getBuffer());

        outputStream.write("bar".getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.write("\n".getBytes());
        output = console.read(null);
        assertEquals("barfoo", output.getBuffer());

        outputStream.write("le".getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.write("\n".getBytes());
        output = console.read(null);
        assertEquals("less:", output.getBuffer());

        console.stop();
    }

}
