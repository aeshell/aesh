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
package org.jboss.aesh.console.eof;

import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.aesh.AeshCommandCompletionTest;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.TestTerminal;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class IgnoreEofTest extends BaseConsoleTest {

    @Test
    public void ignoreeofDefaultVi() throws Exception {
        SettingsBuilder builder = new SettingsBuilder();
        builder.enableAlias(true);
        builder.persistAlias(false);
        builder.persistHistory(false);
        builder.mode(Mode.VI);

        invokeTestConsole(1, new Setup() {
            @Override
            public void call(Console console, OutputStream out) throws Exception {
                console.getExportManager().addVariable("export ignoreeof = 2");

                String BUF = "asdfasdf";

                out.write(BUF.getBytes());
                out.flush();
                Thread.sleep(100);

                assertEquals(BUF, console.getBuffer());

                out.write(Key.CTRL_D.getFirstValue());
                out.flush();
                Thread.sleep(100);

                assertEquals("", console.getBuffer());

                out.write(Key.CTRL_D.getFirstValue());
                out.flush();
                Thread.sleep(100);

                assertTrue(console.isRunning());

                out.write(Key.CTRL_D.getFirstValue());
                out.flush();
                Thread.sleep(100);

                out.write(Key.CTRL_D.getFirstValue());
                out.flush();
                Thread.sleep(100);

                 assertFalse(console.isRunning());
            }
        }, new Verify() {
           @Override
           public int call(Console console, ConsoleOperation op) {
               return 0;
           }
        }, builder);
    }

    @Test
    public void ignoreeofDefaultEmacs() throws Exception {
        SettingsBuilder builder = new SettingsBuilder();
        builder.enableAlias(true);
        builder.persistAlias(false);
        builder.persistHistory(false);
        builder.mode(Mode.EMACS);

        invokeTestConsole(1, new Setup() {
            @Override
            public void call(Console console, OutputStream out) throws Exception {
                console.getExportManager().addVariable("export ignoreeof = 1");

                String BUF = "a";

                out.write(BUF.getBytes());
                out.flush();
                Thread.sleep(100);

                assertEquals(BUF, console.getBuffer());

                out.write(Key.CTRL_D.getFirstValue());
                out.flush();
                Thread.sleep(100);

                assertTrue(console.isRunning());

                out.write(Key.ENTER.getFirstValue());
                out.flush();
                Thread.sleep(100);

                out.write(Key.CTRL_D.getFirstValue());
                out.flush();
                Thread.sleep(100);

                assertTrue(console.isRunning());

                out.write(Key.CTRL_D.getFirstValue());
                out.flush();
                Thread.sleep(100);

                assertFalse(console.isRunning());
            }
        }, new Verify() {
           @Override
           public int call(Console console, ConsoleOperation op) {
               return 0;
           }
        }, builder);
    }


    @Test
    public void resetEOF() throws Exception {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .setPersistExport(false)
                .logging(true)
                .enableExport(true)
                .persistHistory(false)
                .mode(Mode.VI)
                .create();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(AeshCommandCompletionTest.FooCommand.class)
                .create();

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry)
                .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();
        aeshConsole.start();

        outputStream.write(("export ignoreeof = 2").getBytes());
        outputStream.flush();
        Thread.sleep(100);

        outputStream.write(Key.CTRL_D.getFirstValue());
        outputStream.flush();
        Thread.sleep(100);

        outputStream.write(Key.CTRL_D.getFirstValue());
        outputStream.flush();
        Thread.sleep(100);

        outputStream.write(("foo").getBytes());
        outputStream.flush();
        Thread.sleep(100);

        outputStream.write(Key.CTRL_D.getFirstValue());
        outputStream.flush();
        Thread.sleep(100);

        outputStream.write(Key.CTRL_D.getFirstValue());
        outputStream.flush();
        Thread.sleep(100);

        outputStream.write(Key.CTRL_D.getFirstValue());
        outputStream.flush();
        Thread.sleep(100);

        outputStream.write(Key.CTRL_D.getFirstValue());
        outputStream.flush();
        Thread.sleep(100);

        assertFalse(aeshConsole.isRunning());

    }
}
