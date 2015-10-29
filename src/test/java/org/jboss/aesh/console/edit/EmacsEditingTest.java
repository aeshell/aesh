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
package org.jboss.aesh.console.edit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.jboss.aesh.console.AeshConsoleBufferBuilder;
import org.jboss.aesh.console.AeshInputProcessorBuilder;
import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleBuffer;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.InputProcessor;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.Shell;
import org.jboss.aesh.console.TestShell;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.EmacsEditMode;
import org.jboss.aesh.edit.KeyOperationFactory;
import org.jboss.aesh.edit.KeyOperationManager;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class EmacsEditingTest extends BaseConsoleTest {

    @Test
    public void testEmacs() throws Exception {
        Assume.assumeTrue(Config.isOSPOSIXCompatible());

        invokeTestConsole(new Setup() {
            @Override
            public void call(Console console, OutputStream out) throws IOException {
                out.write("34".getBytes());
                //home
                out.write(new byte[]{1});
                out.write(("12"+Config.getLineSeparator()).getBytes());
            }
        }, new Verify() {
           @Override
           public int call(Console console, ConsoleOperation op) {
               assertEquals("1234", op.getBuffer());
               return 0;
           }
        });
    }

    @Test
    public void testOperationParser() {
        Assume.assumeTrue(Config.isOSPOSIXCompatible());

        KeyOperationManager keyOperationManager = new KeyOperationManager();
        keyOperationManager.addOperations(KeyOperationFactory.generateEmacsMode());

        EmacsEditMode editMode = new EmacsEditMode(keyOperationManager);

        Operation operation = editMode.parseInput(Key.ESC, "12345");

        assertEquals(Operation.NO_ACTION, operation);
    }

    @Test
    public void testSearch() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
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

        consoleBuffer.writeString("asdf jkl");
        inputProcessor.parseOperation(Key.ENTER);

        consoleBuffer.writeString("footing");
        inputProcessor.parseOperation(Key.ENTER);

        inputProcessor.parseOperation(Key.CTRL_R);
        inputProcessor.parseOperation(Key.a);

        assertEquals("asdf jkl",
                inputProcessor.parseOperation(Key.ENTER));

    }

    @Test
    public void testSearchWithArrownRight() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
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
                .enableSearch(true)
                .create();

        consoleBuffer.writeString("asdf jkl");
        inputProcessor.parseOperation(Key.ENTER);

        consoleBuffer.writeString("footing");
        inputProcessor.parseOperation(Key.ENTER);

        inputProcessor.parseOperation(Key.CTRL_R);
        inputProcessor.parseOperation(Key.a);
        inputProcessor.parseOperation(Key.RIGHT);

        assertEquals("asdf jkl", consoleBuffer.getBuffer().getLine());
    }

    @Test
    public void testSearchWithArrownLeft() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
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
                .enableSearch(true)
                .create();

        consoleBuffer.writeString("asdf jkl");
        inputProcessor.parseOperation(Key.ENTER);

        consoleBuffer.writeString("footing");
        inputProcessor.parseOperation(Key.ENTER);

        inputProcessor.parseOperation(Key.CTRL_R);
        inputProcessor.parseOperation(Key.a);
        inputProcessor.parseOperation(Key.LEFT);

        assertEquals("asdf jkl", consoleBuffer.getBuffer().getLine());
    }

    @Test
    public void testSearchWithArrownUp() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
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
                .enableSearch(true)
                .create();

        consoleBuffer.writeString("asdf");
        inputProcessor.parseOperation(Key.ENTER);

        consoleBuffer.writeString("footing");
        inputProcessor.parseOperation(Key.ENTER);

        inputProcessor.parseOperation(Key.CTRL_R);
        inputProcessor.parseOperation(Key.f);
        inputProcessor.parseOperation(Key.UP);

        assertEquals("asdf", consoleBuffer.getBuffer().getLine());

    }

    @Test
    public void testSearchWithArrownDown() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
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
                .enableSearch(true)
                .create();

        consoleBuffer.writeString("asdf jkl");
        inputProcessor.parseOperation(Key.ENTER);

        consoleBuffer.writeString("footing");
        inputProcessor.parseOperation(Key.ENTER);

        inputProcessor.parseOperation(Key.CTRL_R);
        inputProcessor.parseOperation(Key.a);
        inputProcessor.parseOperation(Key.DOWN);

        assertEquals("footing", consoleBuffer.getBuffer().getLine());
    }

}
