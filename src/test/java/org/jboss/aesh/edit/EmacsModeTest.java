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
package org.jboss.aesh.edit;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.jboss.aesh.AeshTestCase;
import org.jboss.aesh.TestBuffer;
import org.jboss.aesh.console.AeshConsoleBufferBuilder;
import org.jboss.aesh.console.AeshInputProcessorBuilder;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.ConsoleBuffer;
import org.jboss.aesh.console.InputProcessor;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.TestShell;
import org.jboss.aesh.console.command.CommandOperation;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.Shell;
import org.jboss.aesh.terminal.TestTerminal;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class EmacsModeTest extends AeshTestCase {

    @Test
    public void testSimpleMovementAndEdit() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .readInputrc(false)
                .ansi(true)
                .persistHistory(false)
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

    @Test
    public void testWordMovementAndEdit() throws Exception {

        if(Config.isOSPOSIXCompatible()) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .persistHistory(false)
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

    @Test
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
        assertEqualsBuffer("foo   bar...  Foo-ar.", b);

        b = new TestBuffer("foo   bar...  Foo-Bar.");
        b.append(moveBeginning.getKeyValues())
                .append(moveNextChar.getKeyValues())
                .append(moveNextChar.getKeyValues())
                .append(deletePrevChar.getKeyValues())
                .append(TestBuffer.getNewLine());
        assertEqualsBuffer("fo   bar...  Foo-Bar.", b);
    }


}
