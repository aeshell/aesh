/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit;

import org.jboss.aesh.AeshTestCase;
import org.jboss.aesh.TestBuffer;
import org.jboss.aesh.console.AeshConsoleBufferBuilder;
import org.jboss.aesh.console.AeshInputProcessorBuilder;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.ConsoleBuffer;
import org.jboss.aesh.console.InputProcessor;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.CommandOperation;
import org.jboss.aesh.console.reader.AeshStandardStream;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.CursorPosition;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.Shell;
import org.jboss.aesh.terminal.TerminalSize;
import org.jboss.aesh.terminal.TestTerminal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class EmacsModeTest extends AeshTestCase {

    public EmacsModeTest(String test) {
        super(test);
    }

    public void testSimpleMovementAndEdit() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
                .mode(Mode.EMACS)
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


        consoleBuffer.writeString("1234");

        //remove the last char
        inputProcessor.parseOperation(new CommandOperation(Key.CTRL_D));
        assertEquals("1234", consoleBuffer.getBuffer().getLineNoMask());

        //move on char to the left
        inputProcessor.parseOperation(new CommandOperation(Key.LEFT));
        inputProcessor.parseOperation(new CommandOperation(Key.CTRL_D));
        inputProcessor.parseOperation(new CommandOperation(Key.FIVE));

        assertEquals("1235", consoleBuffer.getBuffer().getLineNoMask());

        inputProcessor.parseOperation(new CommandOperation(Key.CTRL_A));
        inputProcessor.parseOperation(new CommandOperation(Key.CTRL_D));
        assertEquals("235", consoleBuffer.getBuffer().getLineNoMask());
        inputProcessor.parseOperation(new CommandOperation(Key.CTRL_F));
        inputProcessor.parseOperation(new CommandOperation(Key.CTRL_F));
        inputProcessor.parseOperation(new CommandOperation(Key.SIX));

        assertEquals("2365", consoleBuffer.getBuffer().getLineNoMask());

    }

    public void testWordMovementAndEdit() throws Exception {

        if(Config.isOSPOSIXCompatible()) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
                .mode(Mode.EMACS)
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

            inputProcessor.parseOperation(new CommandOperation(Key.META_b));
            inputProcessor.parseOperation(new CommandOperation(Key.META_d));

            String output = inputProcessor.parseOperation(new CommandOperation(Key.ENTER));

            assertEquals("foo  bar...  Foo-.", output);

            consoleBuffer.writeString("foo  bar...  Foo-Bar.");
            inputProcessor.parseOperation(new CommandOperation(Key.CTRL_A));
            inputProcessor.parseOperation(new CommandOperation(Key.META_f));
            inputProcessor.parseOperation(new CommandOperation(Key.META_f));
            inputProcessor.parseOperation(new CommandOperation(Key.META_d));

            output = inputProcessor.parseOperation(new CommandOperation(Key.ENTER));

            assertEquals("foo  bar-Bar.", output);

            consoleBuffer.writeString("foo  bar...  Foo-Bar.");
            inputProcessor.parseOperation(new CommandOperation(Key.CTRL_A));
            inputProcessor.parseOperation(new CommandOperation(Key.META_f));
            inputProcessor.parseOperation(new CommandOperation(Key.META_f));
            inputProcessor.parseOperation(new CommandOperation(Key.CTRL_U));

            output = inputProcessor.parseOperation(new CommandOperation(Key.ENTER));

            assertEquals("...  Foo-Bar.", output);
        }
    }

    public void testArrowMovement() throws Exception {

        KeyOperation deletePrevChar =  new KeyOperation(Key.CTRL_H, Operation.DELETE_PREV_CHAR);
        KeyOperation moveBeginning = new KeyOperation(Key.CTRL_A, Operation.MOVE_BEGINNING);
        KeyOperation movePrevChar;
        KeyOperation moveNextChar;
        moveNextChar = new KeyOperation(Key.RIGHT, Operation.MOVE_NEXT_CHAR);
        movePrevChar = new KeyOperation(Key.LEFT, Operation.MOVE_PREV_CHAR);

        TestBuffer b = new TestBuffer("foo   bar...  Foo-Bar.");
        b.append(movePrevChar.getKeyValues())
                .append(movePrevChar.getKeyValues())
                .append(movePrevChar.getKeyValues())
                .append(deletePrevChar.getKeyValues())
                .append(TestBuffer.getNewLine());
        assertEquals("foo   bar...  Foo-ar.", b);

        b = new TestBuffer("foo   bar...  Foo-Bar.");
        b.append(moveBeginning.getKeyValues())
                .append(moveNextChar.getKeyValues())
                .append(moveNextChar.getKeyValues())
                .append(deletePrevChar.getKeyValues())
                .append(TestBuffer.getNewLine());
        assertEquals("fo   bar...  Foo-Bar.", b);
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
