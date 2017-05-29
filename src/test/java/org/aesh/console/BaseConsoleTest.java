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
package org.aesh.console;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.aesh.console.settings.Settings;
import org.aesh.console.settings.SettingsBuilder;
import org.aesh.readline.Prompt;
import org.aesh.readline.ReadlineConsole;
import org.aesh.utils.Config;
import org.aesh.tty.TestConnection;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public abstract class BaseConsoleTest {

    private TestConnection testConnection;

    Settings getDefaultSettings(SettingsBuilder builder) {
        if(builder == null) {
            builder = SettingsBuilder.builder();
            builder.enableAlias(false);
            builder.persistHistory(false);
        }
        if(testConnection == null)
            testConnection = new TestConnection();
        builder.connection(testConnection);
        builder.readInputrc(false);
        //builder.inputStream(is);
        builder.persistAlias(false);
        //builder.outputStream(new PrintStream(new ByteArrayOutputStream()));

        return builder.build();
    }

    ReadlineConsole getTestConsole(SettingsBuilder builder) throws IOException {
        return new ReadlineConsole(getDefaultSettings(builder));
    }

    ReadlineConsole getTestConsole() throws IOException {
        return new ReadlineConsole(getDefaultSettings(null));
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
        //PipedOutputStream outputStream = new PipedOutputStream();
        //PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        //CountDownLatch latch = new CountDownLatch(callbackCount);
        //List<Throwable> exceptions = new ArrayList<Throwable>();

        ReadlineConsole consoleSetup = null;
        if(settings != null) {
            consoleSetup = getTestConsole(settings);
        }
        else {
            consoleSetup = getTestConsole();
        }
        final ReadlineConsole console = consoleSetup;

        console.start();
        /*
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
        */

        Queue<String> in = new LinkedList<>();
        Consumer<String> input = in::add;

        Supplier<String> output = () -> {
            if(!in.isEmpty())
                testConnection.read(in.remove());
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            return testConnection.getOutputBuffer();
        };

        ConsoleInteraction consoleInteraction = new ConsoleInteraction() {
            @Override
            public void setPrompt(Prompt prompt) {
                console.setPrompt(prompt);
            }

            @Override
            public Prompt prompt() {
                return console.prompt();
            }
        };


        setup.call(consoleInteraction, input);
        Thread.sleep(100);
        verify.call(consoleInteraction, output);

        console.stop();
        /*
        if (!latch.await(5000, TimeUnit.MILLISECONDS)) {
            fail("Failed waiting for Console to finish");
        }
        console.stop();
        if (exceptions.size() > 0) {
            throw new RuntimeException(exceptions.get(0));
        }
        */
    }

    public interface Setup {
        void call(ConsoleInteraction console, Consumer<String> input) throws Exception;
    }

    public interface Verify {
        int call(ConsoleInteraction console, Supplier<String> output) throws Exception;
    }

    public interface ConsoleInteraction {
        void setPrompt(Prompt prompt);

        Prompt prompt();
    }
}
