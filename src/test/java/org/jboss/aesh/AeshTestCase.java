/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ackage org.jboss.aesh;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.jboss.aesh.console.AeshConsoleCallback;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.TestConsoleCallback;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.terminal.TestTerminal;

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
        console.setConsoleCallback(new AeshConsoleCallback() {
            @Override
            public int execute(ConsoleOperation output) throws InterruptedException {
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

    public void assertEqualsViMode(final String expected, TestBuffer buffer) throws Exception {

        SettingsBuilder builder = new SettingsBuilder();
        builder.readInputrc(false);
        builder.terminal(new TestTerminal());
        builder.inputStream(new ByteArrayInputStream(buffer.getBytes()));
        builder.outputStream(new PrintStream(new ByteArrayOutputStream()));
        builder.readAhead(false);
        builder.mode(Mode.VI);
        if(!Config.isOSPOSIXCompatible())
            builder.ansi(false);

        CountDownLatch latch = new CountDownLatch(1);
        List<Throwable> exceptions = new ArrayList<Throwable>();

        Console console = new Console(builder.create());
        console.setConsoleCallback(new TestConsoleCallback(latch, exceptions) {
            @Override
            public int verify(ConsoleOperation output) {
                assertEquals(expected, output.getBuffer());
                return 0;
            }
        });


        if(!latch.await(200, TimeUnit.MILLISECONDS)) {
           fail("Failed waiting for Console to finish");
        }
        console.stop();
        if(exceptions.size() > 0) {
           throw new RuntimeException(exceptions.get(0));
        }
    }

}
