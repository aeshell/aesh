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
 */
package org.jboss.aesh.edit;

import org.jboss.aesh.console.AeshConsoleBufferBuilder;
import org.jboss.aesh.console.AeshInputProcessorBuilder;
import org.jboss.aesh.console.ConsoleBuffer;
import org.jboss.aesh.console.InputProcessor;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.CommandOperation;
import org.jboss.aesh.console.reader.AeshStandardStream;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.terminal.CursorPosition;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.Shell;
import org.jboss.aesh.terminal.TerminalSize;
import org.jboss.aesh.terminal.TestTerminal;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ViModeTest {

    @Test
    public void testSimpleMovementAndEdit() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
                .mode(Mode.VI)
                .create();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder()
                .shell(shell)
                .prompt(new Prompt("aesh"))
                .editMode(settings.getEditMode())
                .create();

        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .settings(settings)
                .create();

        consoleBuffer.writeString("abcd");

        CommandOperation co = new CommandOperation(Key.ESC);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.x);
        inputProcessor.parseOperation(co);

        assertEquals("abc", consoleBuffer.getBuffer().getLineNoMask());

        co = new CommandOperation(Key.h);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.s);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.T);
        inputProcessor.parseOperation(co);

        assertEquals("aTc", consoleBuffer.getBuffer().getLineNoMask());

        co = new CommandOperation(Key.ESC);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.ZERO);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.x);
        inputProcessor.parseOperation(co);

        assertEquals("Tc", consoleBuffer.getBuffer().getLineNoMask());

        co = new CommandOperation(Key.l);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.a);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.o);
        inputProcessor.parseOperation(co);

        assertEquals("Tco", consoleBuffer.getBuffer().getLineNoMask());

    }

    @Test
    public void testWordMovementAndEdit() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
                .mode(Mode.VI)
                .create();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder()
                .shell(shell)
                .prompt(new Prompt("aesh"))
                .editMode(settings.getEditMode())
                .create();

        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .settings(settings)
                .create();

        consoleBuffer.writeString("foo  bar...  Foo-Bar.");

        CommandOperation co = new CommandOperation(Key.ESC);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.B);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.d);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.b);
        inputProcessor.parseOperation(co);

        assertEquals("foo  barFoo-Bar.", consoleBuffer.getBuffer().getLineNoMask());

        co = new CommandOperation(Key.ESC);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.ZERO);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.W);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.d);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.w);
        inputProcessor.parseOperation(co);

        assertEquals("foo  -Bar.", consoleBuffer.getBuffer().getLineNoMask());

    }

    @Test
    public void testRepeatAndEdit() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
                .mode(Mode.VI)
                .create();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder()
                .shell(shell)
                .prompt(new Prompt("aesh"))
                .editMode(settings.getEditMode())
                .create();

        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .settings(settings)
                .create();

        consoleBuffer.writeString("/cd /home/foo/ ls/ cd Desktop/ ls ../");

        CommandOperation co = new CommandOperation(Key.ESC);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.ZERO);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.w);
        inputProcessor.parseOperation(co);
        inputProcessor.parseOperation(co);
        inputProcessor.parseOperation(co);
        inputProcessor.parseOperation(co);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.c);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.w);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.b);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.a);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.r);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.ESC);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.W);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.d);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.w);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.PERIOD);
        inputProcessor.parseOperation(co);
        co = new CommandOperation(Key.ENTER);

        assertEquals("/cd /home/bar/ cd Desktop/ ls ../", inputProcessor.parseOperation(co));

        consoleBuffer.writeString("/cd /home/foo/ ls/ cd Desktop/ ls ../");
        inputProcessor.parseOperation(new CommandOperation(Key.ESC));
        inputProcessor.parseOperation(new CommandOperation(Key.B));
        inputProcessor.parseOperation(new CommandOperation(Key.D));
        inputProcessor.parseOperation(new CommandOperation(Key.B));
        inputProcessor.parseOperation(new CommandOperation(Key.PERIOD));
        inputProcessor.parseOperation(new CommandOperation(Key.B));
        inputProcessor.parseOperation(new CommandOperation(Key.PERIOD));

        assertEquals("/cd /home/foo/ ls/ cd ",
                inputProcessor.parseOperation(new CommandOperation(Key.ENTER)));
    }

    @Test
    public void testTildeAndEdit() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
                .mode(Mode.VI)
                .create();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder()
                .shell(shell)
                .prompt(new Prompt("aesh"))
                .editMode(settings.getEditMode())
                .create();

        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .settings(settings)
                .create();

        consoleBuffer.writeString("apt-get install vIM");
        inputProcessor.parseOperation(new CommandOperation(Key.ESC));
        inputProcessor.parseOperation(new CommandOperation(Key.b));
        inputProcessor.parseOperation(new CommandOperation(Key.TILDE));
        inputProcessor.parseOperation(new CommandOperation(Key.TILDE));
        inputProcessor.parseOperation(new CommandOperation(Key.TILDE));
        inputProcessor.parseOperation(new CommandOperation(Key.ZERO));
        inputProcessor.parseOperation(new CommandOperation(Key.w));
        inputProcessor.parseOperation(new CommandOperation(Key.w));
        inputProcessor.parseOperation(new CommandOperation(Key.c));
        inputProcessor.parseOperation(new CommandOperation(Key.w));

        consoleBuffer.writeString("cache");

        inputProcessor.parseOperation(new CommandOperation(Key.ESC));
        inputProcessor.parseOperation(new CommandOperation(Key.w));
        inputProcessor.parseOperation(new CommandOperation(Key.c));
        inputProcessor.parseOperation(new CommandOperation(Key.w));

        consoleBuffer.writeString("search");

        assertEquals("apt-cache search Vim",
                inputProcessor.parseOperation(new CommandOperation(Key.ENTER)));

    }

    @Test
    public void testPasteAndEdit() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
                .mode(Mode.VI)
                .create();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder()
                .shell(shell)
                .prompt(new Prompt("aesh"))
                .editMode(settings.getEditMode())
                .create();

        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .settings(settings)
                .create();

        consoleBuffer.writeString("apt-get install vIM");

        inputProcessor.parseOperation(new CommandOperation(Key.ESC));
        inputProcessor.parseOperation(new CommandOperation(Key.ZERO));
        inputProcessor.parseOperation(new CommandOperation(Key.d));
        inputProcessor.parseOperation(new CommandOperation(Key.W));
        inputProcessor.parseOperation(new CommandOperation(Key.w));
        inputProcessor.parseOperation(new CommandOperation(Key.P));
        inputProcessor.parseOperation(new CommandOperation(Key.W));
        inputProcessor.parseOperation(new CommandOperation(Key.y));
        inputProcessor.parseOperation(new CommandOperation(Key.w));
        inputProcessor.parseOperation(new CommandOperation(Key.DOLLAR));
        inputProcessor.parseOperation(new CommandOperation(Key.p));

        assertEquals("install apt-get vIMvIM",
                inputProcessor.parseOperation(new CommandOperation(Key.ENTER)));
    }

    @Test
    public void testSearch() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .persistHistory(false)
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
                .mode(Mode.VI)
                .create();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder()
                .shell(shell)
                .prompt(new Prompt("aesh"))
                .editMode(settings.getEditMode())
                .create();

        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .settings(settings)
                .create();

        consoleBuffer.writeString("asdf jkl");
        inputProcessor.parseOperation(new CommandOperation(Key.ENTER));

        consoleBuffer.writeString("footing");
        inputProcessor.parseOperation(new CommandOperation(Key.ENTER));

        inputProcessor.parseOperation(new CommandOperation(Key.CTRL_R));
        inputProcessor.parseOperation(new CommandOperation(Key.a));

        assertEquals("asdf jkl",
        inputProcessor.parseOperation(new CommandOperation(Key.ENTER)));

    }

    private static class TestShell implements Shell {

        private final PrintStream out;
        private final PrintStream err;

        TestShell(PrintStream out, PrintStream err) {
            this.out = out;
            this.err = err;
        }

        @Override
        public void clear() throws IOException {

        }

        @Override
        public PrintStream out() {
            return out;
        }

        @Override
        public PrintStream err() {
            return err;
        }

        @Override
        public AeshStandardStream in() {
            return null;
        }

        @Override
        public TerminalSize getSize() {
            return new TerminalSize(80,20);
        }

        @Override
        public CursorPosition getCursor() {
            return new CursorPosition(1,1);
        }

        @Override
        public void setCursor(CursorPosition position) {

        }

        @Override
        public void moveCursor(int rows, int columns) {

        }

        @Override
        public boolean isMainBuffer() {
            return false;
        }

        @Override
        public void enableAlternateBuffer() {

        }

        @Override
        public void enableMainBuffer() {

        }
    }
}
