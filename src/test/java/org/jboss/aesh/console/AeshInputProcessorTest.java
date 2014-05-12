/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

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
import static org.junit.Assert.assertNull;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshInputProcessorTest {

    @Test
    public void testEditOperation() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
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

        CommandOperation operation = new CommandOperation(Key.a);

        String result = inputProcessor.parseOperation(operation);

        assertNull(result);
        assertEquals("a", consoleBuffer.getBuffer().getLine());

        operation = new CommandOperation(Key.e);
        inputProcessor.parseOperation(operation);
        operation = new CommandOperation(Key.s);
        inputProcessor.parseOperation(operation);
        operation = new CommandOperation(Key.h);
        inputProcessor.parseOperation(operation);

        assertEquals("aesh", consoleBuffer.getBuffer().getLine());

        operation = new CommandOperation(Key.ENTER);
        result = inputProcessor.parseOperation(operation);
        assertEquals("aesh", result);
    }

    @Test
    public void testMovement() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
                .create();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder().shell(shell).prompt(new Prompt("aesh")).create();

        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .settings(settings)
                .create();

        CommandOperation edit = new CommandOperation(Key.e);

        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.h);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.LEFT);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.s);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.RIGHT);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.r);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.u);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.l);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.e);
        inputProcessor.parseOperation(edit);

        edit = new CommandOperation(Key.CTRL_A);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.a);
        inputProcessor.parseOperation(edit);

        edit = new CommandOperation(Key.CTRL_E);
        inputProcessor.parseOperation(edit);

        edit = new CommandOperation(Key.s);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.ENTER);
        String result = inputProcessor.parseOperation(edit);

        assertEquals("aesh rules", result);
    }

    @Test
    public void testHistory() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .disableHistory(false)
                .persistHistory(false)
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
                .create();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder().shell(shell).prompt(new Prompt("aesh")).create();

        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .settings(settings)
                .create();

        CommandOperation edit = new CommandOperation(Key.f);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.o);
        inputProcessor.parseOperation(edit);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.b);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.a);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.r);
        inputProcessor.parseOperation(edit);

        edit = new CommandOperation(Key.ENTER);
        String result = inputProcessor.parseOperation(edit);

        assertEquals("foo bar", result);

        assertEquals("", consoleBuffer.getBuffer().getLine());

        edit = new CommandOperation(Key.UP);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.BACKSPACE);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.ENTER);
        result = inputProcessor.parseOperation(edit);
        assertEquals("foo ba", result);

        edit = new CommandOperation(Key.UP);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.DOWN);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.ENTER);
        result = inputProcessor.parseOperation(edit);
        assertEquals("", result);

    }

    @Test
    public void testUndo() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
                .create();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder().shell(shell).prompt(new Prompt("aesh")).create();

        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .settings(settings)
                .create();

        CommandOperation edit = new CommandOperation(Key.f);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.o);
        inputProcessor.parseOperation(edit);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.b);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.a);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.r);
        inputProcessor.parseOperation(edit);

        edit = new CommandOperation(Key.BACKSPACE);
        inputProcessor.parseOperation(edit);
        inputProcessor.parseOperation(edit);
        inputProcessor.parseOperation(edit);

        edit = new CommandOperation(Key.CTRL_X_CTRL_U);
        inputProcessor.parseOperation(edit);
        inputProcessor.parseOperation(edit);

        edit = new CommandOperation(Key.ENTER);
        String result = inputProcessor.parseOperation(edit);
        assertEquals("foo ba", result);
    }

    @Test
    public void testSearch() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder().shell(shell).prompt(new Prompt("aesh")).create();

        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .enableHistory(true)
                .persistHistory(false)
                .historySize(10)
                .enableSearch(true)
                .create();

        CommandOperation edit = new CommandOperation(Key.f);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.o);
        inputProcessor.parseOperation(edit);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.b);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.a);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.r);
        inputProcessor.parseOperation(edit);

        edit = new CommandOperation(Key.ENTER);
        String result = inputProcessor.parseOperation(edit);
        assertEquals("foo bar", result);

        result = inputProcessor.parseOperation(edit);
        assertEquals("", result);

        edit = new CommandOperation(Key.CTRL_R);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.f);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.ENTER);
        result = inputProcessor.parseOperation(edit);
        assertEquals("foo bar", result);
    }

    @Test
    public void testPrompt() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
                .persistHistory(false)
                .create();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder().shell(shell).prompt(new Prompt("aesh")).create();

        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .settings(settings)
                .create();

        CommandOperation edit = new CommandOperation(Key.f);

    }

    @Test
    public void testCapitalizeWord() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
                .create();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder().shell(shell).prompt(new Prompt("aesh")).create();

        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .settings(settings)
                .create();

        CommandOperation edit = new CommandOperation(Key.f);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.o);
        inputProcessor.parseOperation(edit);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.b);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.a);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.r);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.META_c);
        inputProcessor.parseOperation(edit);
        //line should be the same
        assertEquals("foo bar ", consoleBuffer.getBuffer().getLineNoMask());

        edit = new CommandOperation(Key.LEFT);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.META_c);
        inputProcessor.parseOperation(edit);
        assertEquals("foo Bar ", consoleBuffer.getBuffer().getLineNoMask());
        edit = new CommandOperation(Key.LEFT);
        inputProcessor.parseOperation(edit);
        inputProcessor.parseOperation(edit);
        inputProcessor.parseOperation(edit);
        inputProcessor.parseOperation(edit);
        inputProcessor.parseOperation(edit);

        edit = new CommandOperation(Key.META_c);
        inputProcessor.parseOperation(edit);
        assertEquals("Foo Bar ", consoleBuffer.getBuffer().getLineNoMask());
    }

    @Test
    public void testLowerCaseWord() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
                .create();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder().shell(shell).prompt(new Prompt("aesh")).create();

        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .settings(settings)
                .create();

        CommandOperation edit = new CommandOperation(Key.F);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.o);
        inputProcessor.parseOperation(edit);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.B);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.A);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.r);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.META_l);
        inputProcessor.parseOperation(edit);
        //line should be the same
        assertEquals("Foo BAr ", consoleBuffer.getBuffer().getLineNoMask());

        edit = new CommandOperation(Key.LEFT);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.META_l);
        inputProcessor.parseOperation(edit);
        assertEquals("Foo bar ", consoleBuffer.getBuffer().getLineNoMask());
        edit = new CommandOperation(Key.LEFT);
        inputProcessor.parseOperation(edit);
        inputProcessor.parseOperation(edit);
        inputProcessor.parseOperation(edit);
        inputProcessor.parseOperation(edit);
        inputProcessor.parseOperation(edit);

        edit = new CommandOperation(Key.META_l);
        inputProcessor.parseOperation(edit);
        assertEquals("foo bar ", consoleBuffer.getBuffer().getLineNoMask());
    }

    @Test
    public void testUpperCaseWord() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
                .create();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder().shell(shell).prompt(new Prompt("aesh")).create();

        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .settings(settings)
                .create();

        CommandOperation edit = new CommandOperation(Key.f);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.o);
        inputProcessor.parseOperation(edit);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.b);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.a);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.r);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.META_u);
        inputProcessor.parseOperation(edit);
        //line should be the same
        assertEquals("foo bar ", consoleBuffer.getBuffer().getLineNoMask());

        edit = new CommandOperation(Key.LEFT);
        inputProcessor.parseOperation(edit);
        edit = new CommandOperation(Key.META_u);
        inputProcessor.parseOperation(edit);
        assertEquals("foo BAR ", consoleBuffer.getBuffer().getLineNoMask());
        edit = new CommandOperation(Key.CTRL_A);
        inputProcessor.parseOperation(edit);

        edit = new CommandOperation(Key.META_u);
        inputProcessor.parseOperation(edit);
        assertEquals("FOO BAR ", consoleBuffer.getBuffer().getLineNoMask());
    }

    @Test
    public void testQuotes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
                .create();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder().shell(shell).prompt(new Prompt("aesh")).create();

        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .settings(settings)
                .create();

        inputProcessor.parseOperation(new CommandOperation(Key.f));
        inputProcessor.parseOperation(new CommandOperation(Key.o));
        inputProcessor.parseOperation(new CommandOperation(Key.SPACE));
        inputProcessor.parseOperation(new CommandOperation(Key.QUOTE));
        String result = inputProcessor.parseOperation(new CommandOperation(Key.ENTER));

        assertEquals(null, result);

        inputProcessor.parseOperation(new CommandOperation(Key.o));
        result = inputProcessor.parseOperation(new CommandOperation(Key.ENTER));
        assertEquals(null, result);

        inputProcessor.parseOperation(new CommandOperation(Key.QUOTE));
        result = inputProcessor.parseOperation(new CommandOperation(Key.ENTER));
        assertEquals("fo \"o\"", result);

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
