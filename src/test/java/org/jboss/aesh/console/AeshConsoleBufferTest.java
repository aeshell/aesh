/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.reader.AeshStandardStream;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.edit.actions.Action;
import org.jboss.aesh.edit.actions.DeleteAction;
import org.jboss.aesh.edit.actions.NextWordAction;
import org.jboss.aesh.edit.actions.PrevSpaceWordAction;
import org.jboss.aesh.edit.actions.PrevWordAction;
import org.jboss.aesh.edit.actions.SimpleAction;
import org.jboss.aesh.terminal.CursorPosition;
import org.jboss.aesh.terminal.Shell;
import org.jboss.aesh.terminal.TerminalSize;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshConsoleBufferTest {

    @Test
    public void testSimpleWrites() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder().shell(shell).prompt(new Prompt("aesh")).create();

        consoleBuffer.displayPrompt();
        assertTrue(byteArrayOutputStream.toString().contains("aesh"));
        byteArrayOutputStream.reset();

        consoleBuffer.writeString("foo");
        assertEquals("foo", byteArrayOutputStream.toString());


        consoleBuffer.writeString("OOO");
        assertEquals("fooOOO", byteArrayOutputStream.toString());
    }

    @Test
    public void testMovement()  throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder().shell(shell).prompt(new Prompt("aesh")).create();

        consoleBuffer.writeString("foo0");
        consoleBuffer.moveCursor(-1);
        assertEquals("foo0" + new String(Buffer.printAnsi("1D")), byteArrayOutputStream.toString());
        consoleBuffer.moveCursor(-10);
        assertEquals("foo0" + new String(Buffer.printAnsi("1D")) + new String(Buffer.printAnsi("3D")), byteArrayOutputStream.toString());

        consoleBuffer.writeString("1");
        assertEquals("1foo0", consoleBuffer.getBuffer().getLine());

        byteArrayOutputStream.reset();
        consoleBuffer.moveCursor(1);
        assertEquals(new String(Buffer.printAnsi("1C")), byteArrayOutputStream.toString());

        consoleBuffer.writeString("2");
        assertEquals("1f2oo0", consoleBuffer.getBuffer().getLine());
    }

    @Test
    public void testDelete()  throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder().shell(shell).prompt(new Prompt("aesh")).create();

        consoleBuffer.writeString("foo0");
        consoleBuffer.performAction(new DeleteAction(consoleBuffer.getBuffer().getCursor(), Action.DELETE));
        assertEquals("foo0", consoleBuffer.getBuffer().getLine());
        consoleBuffer.performAction(new DeleteAction(consoleBuffer.getBuffer().getCursor() - 1, Action.DELETE));
        assertEquals("foo", consoleBuffer.getBuffer().getLine());
        consoleBuffer.performAction(new PrevWordAction(consoleBuffer.getBuffer().getCursor(), Action.DELETE, Mode.EMACS));
        assertEquals("", consoleBuffer.getBuffer().getLine());

        consoleBuffer.writeString("foo bar");
        consoleBuffer.performAction(new PrevSpaceWordAction(consoleBuffer.getBuffer().getCursor(), Action.DELETE));
        assertEquals("foo ", consoleBuffer.getBuffer().getLine());

        consoleBuffer.writeString("foo0 bah");
        //move to the beginning
        consoleBuffer.performAction(new SimpleAction(consoleBuffer.getBuffer().getCursor(), Action.MOVE, 0));

        consoleBuffer.performAction(new NextWordAction(consoleBuffer.getBuffer().getCursor(), Action.DELETE, Mode.EMACS));
        assertEquals("foo0 bah", consoleBuffer.getBuffer().getLine());

        consoleBuffer.performAction(new DeleteAction(consoleBuffer.getBuffer().getCursor(), Action.DELETE));
        assertEquals("oo0 bah", consoleBuffer.getBuffer().getLine());
        consoleBuffer.moveCursor(1);
        consoleBuffer.performAction(new DeleteAction(consoleBuffer.getBuffer().getCursor(), Action.DELETE, true));
        assertEquals("o0 bah", consoleBuffer.getBuffer().getLine());
        consoleBuffer.performAction(new SimpleAction(consoleBuffer.getBuffer().getCursor(), Action.DELETE, consoleBuffer.getBuffer().length()));
        assertEquals("", consoleBuffer.getBuffer().getLine());

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
