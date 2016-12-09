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
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aesh.console;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.readline.ReadlineConsole;
import org.aesh.util.Config;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public abstract class BaseConsoleTest {

    /*
    Settings getDefaultSettings(InputStream is, SettingsBuilder builder) {
        if(builder == null) {
            builder = new SettingsBuilder();
            builder.enableAlias(false);
            builder.persistHistory(false);
        }
        builder.readInputrc(false);
        builder.inputStream(is);
        builder.persistAlias(false);
        builder.outputStream(new PrintStream(new ByteArrayOutputStream()));

        return builder.create();
    }

    ReadlineConsole getTestConsole(SettingsBuilder builder, InputStream is) throws IOException {
        return new ReadlineConsole(getDefaultSettings(is, builder));
    }

    ReadlineConsole getTestConsole(InputStream is) throws IOException {
        return new ReadlineConsole(getDefaultSettings(is, null));
    }

    public String getContentOfFile(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(Config.getLineSeparator());
                line = br.readLine();
            }
            return sb.toString();

        }
        finally {
            br.close();
        }
    }

    protected void invokeTestConsole(final Setup setup, final Verify verify) throws Exception {
        invokeTestConsole(1,  setup, verify);
    }

    protected void invokeTestConsole(int callbackCount, final Setup setup, final Verify verify) throws Exception {
        invokeTestConsole(callbackCount, setup, verify, null);
    }

    protected void invokeTestConsole(int callbackCount, final Setup setup, final Verify verify, SettingsBuilder settings) throws Exception {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        CountDownLatch latch = new CountDownLatch(callbackCount);
        List<Throwable> exceptions = new ArrayList<Throwable>();

        ReadlineConsole consoleSetup = null;
        if(settings != null) {
            consoleSetup = getTestConsole(settings, pipedInputStream);
        } else {
            consoleSetup = getTestConsole(pipedInputStream);
        }
        final ReadlineConsole console = consoleSetup;

        console.setConsoleCallback(new TestConsoleCallback(latch, exceptions) {
            @Override
            public int verify(ConsoleOperation op) {
                try {
                    return verify.call(console, op);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        console.start();

        setup.call(console, outputStream);

        if (!latch.await(5000, TimeUnit.MILLISECONDS)) {
            fail("Failed waiting for Console to finish");
        }
        console.stop();
        if (exceptions.size() > 0) {
            throw new RuntimeException(exceptions.get(0));
        }
    }

    public interface Setup {
        void call(ReadlineConsole console, OutputStream out) throws Exception;
    }

    public interface Verify {
        int call(ReadlineConsole console, ConsoleOperation op) throws Exception;
    }
    */
}
