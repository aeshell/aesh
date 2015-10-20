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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.jboss.aesh.console.command.CommandOperation;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.Shell;
import org.jboss.aesh.terminal.TestTerminal;
import org.junit.Test;

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


        String result = inputProcessor.parseOperation(Key.a);

        assertNull(result);
        assertEquals("a", consoleBuffer.getBuffer().getLine());

        inputProcessor.parseOperation(Key.e);
        inputProcessor.parseOperation(Key.s);
        inputProcessor.parseOperation(Key.h);

        assertEquals("aesh", consoleBuffer.getBuffer().getLine());

        result = inputProcessor.parseOperation(Key.ENTER);
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

        inputProcessor.parseOperation(Key.e);
        //edit = new CommandOperation(Key.h);
        inputProcessor.parseOperation(Key.h);
        //edit = new CommandOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.LEFT);
        //edit = new CommandOperation(Key.s);
        inputProcessor.parseOperation(Key.s);
        //edit = new CommandOperation(Key.RIGHT);
        inputProcessor.parseOperation(Key.RIGHT);
        //edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.SPACE);
        //edit = new CommandOperation(Key.r);
        inputProcessor.parseOperation(Key.r);
        //edit = new CommandOperation(Key.u);
        inputProcessor.parseOperation(Key.u);
        //edit = new CommandOperation(Key.l);
        inputProcessor.parseOperation(Key.l);
        //edit = new CommandOperation(Key.e);
        inputProcessor.parseOperation(Key.e);

        //edit = new CommandOperation(Key.CTRL_A);
        inputProcessor.parseOperation(Key.CTRL_A);
        //edit = new CommandOperation(Key.a);
        inputProcessor.parseOperation(Key.a);

        //edit = new CommandOperation(Key.CTRL_E);
        inputProcessor.parseOperation(Key.CTRL_E);

        //edit = new CommandOperation(Key.s);
        inputProcessor.parseOperation(Key.s);
        //edit = new CommandOperation(Key.ENTER);
        String result = inputProcessor.parseOperation(Key.ENTER);

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
        inputProcessor.parseOperation(Key.f);
        edit = new CommandOperation(Key.o);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.o);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.SPACE);
        edit = new CommandOperation(Key.b);
        inputProcessor.parseOperation(Key.b);
        edit = new CommandOperation(Key.a);
        inputProcessor.parseOperation(Key.a);
        edit = new CommandOperation(Key.r);
        inputProcessor.parseOperation(Key.r);

        edit = new CommandOperation(Key.ENTER);
        String result = inputProcessor.parseOperation(Key.ENTER);

        assertEquals("foo bar", result);

        assertEquals("", consoleBuffer.getBuffer().getLine());

        edit = new CommandOperation(Key.UP);
        inputProcessor.parseOperation(Key.UP);
        edit = new CommandOperation(Key.BACKSPACE);
        inputProcessor.parseOperation(Key.BACKSPACE);
        edit = new CommandOperation(Key.ENTER);
        result = inputProcessor.parseOperation(Key.ENTER);
        assertEquals("foo ba", result);

        edit = new CommandOperation(Key.UP);
        inputProcessor.parseOperation(Key.UP);
        edit = new CommandOperation(Key.DOWN);
        inputProcessor.parseOperation(Key.DOWN);
        edit = new CommandOperation(Key.ENTER);
        result = inputProcessor.parseOperation(Key.ENTER);
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
        inputProcessor.parseOperation(Key.f);
        edit = new CommandOperation(Key.o);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.o);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.SPACE);
        edit = new CommandOperation(Key.b);
        inputProcessor.parseOperation(Key.b);
        edit = new CommandOperation(Key.a);
        inputProcessor.parseOperation(Key.a);
        edit = new CommandOperation(Key.r);
        inputProcessor.parseOperation(Key.r);

        edit = new CommandOperation(Key.BACKSPACE);
        inputProcessor.parseOperation(Key.BACKSPACE);
        inputProcessor.parseOperation(Key.BACKSPACE);
        inputProcessor.parseOperation(Key.BACKSPACE);

        edit = new CommandOperation(Key.CTRL_X_CTRL_U);
        inputProcessor.parseOperation(Key.CTRL_X_CTRL_U);
        inputProcessor.parseOperation(Key.CTRL_X_CTRL_U);

        edit = new CommandOperation(Key.ENTER);
        String result = inputProcessor.parseOperation(Key.ENTER);
        assertEquals("foo ba", result);
    }

    @Test
    public void testSearch() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder()
                .ansi(false).shell(shell).prompt(new Prompt("aesh")).create();

        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .enableHistory(true)
                .persistHistory(false)
                .historySize(10)
                .enableSearch(true)
                .create();

        CommandOperation edit = new CommandOperation(Key.f);
        inputProcessor.parseOperation(Key.f);
        edit = new CommandOperation(Key.o);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.o);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.SPACE);
        edit = new CommandOperation(Key.b);
        inputProcessor.parseOperation(Key.b);
        edit = new CommandOperation(Key.a);
        inputProcessor.parseOperation(Key.a);
        edit = new CommandOperation(Key.r);
        inputProcessor.parseOperation(Key.r);
        edit = new CommandOperation(Key.ONE);
        inputProcessor.parseOperation(Key.ONE);

        edit = new CommandOperation(Key.ENTER);
        String result = inputProcessor.parseOperation(Key.ENTER);
        assertEquals("foo bar1", result);

        result = inputProcessor.parseOperation(Key.ENTER);
        assertEquals("", result);

        edit = new CommandOperation(Key.CTRL_R);
        inputProcessor.parseOperation(Key.CTRL_R);
        edit = new CommandOperation(Key.f);
        inputProcessor.parseOperation(Key.f);
        edit = new CommandOperation(Key.ENTER);
        result = inputProcessor.parseOperation(Key.ENTER);
        assertEquals("foo bar1", result);

        edit = new CommandOperation(Key.f);
        inputProcessor.parseOperation(Key.f);
        edit = new CommandOperation(Key.o);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.o);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.SPACE);
        edit = new CommandOperation(Key.b);
        inputProcessor.parseOperation(Key.b);
        edit = new CommandOperation(Key.a);
        inputProcessor.parseOperation(Key.a);
        edit = new CommandOperation(Key.r);
        inputProcessor.parseOperation(Key.r);
        edit = new CommandOperation(Key.TWO);
        inputProcessor.parseOperation(Key.TWO);

        edit = new CommandOperation(Key.ENTER);
        result = inputProcessor.parseOperation(Key.ENTER);
        assertEquals("foo bar2", result);

        edit = new CommandOperation(Key.CTRL_R);
        inputProcessor.parseOperation(Key.CTRL_R);
        edit = new CommandOperation(Key.f);
        inputProcessor.parseOperation(Key.f);
        assertEquals("(reverse-i-search) `f': foo bar2", consoleBuffer.getBuffer().getLine());

        edit = new CommandOperation(Key.CTRL_R);
        inputProcessor.parseOperation(Key.CTRL_R);
        assertEquals("(reverse-i-search) `f': foo bar1", consoleBuffer.getBuffer().getLine());
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
        inputProcessor.parseOperation(Key.f);
        edit = new CommandOperation(Key.o);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.o);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.SPACE);
        edit = new CommandOperation(Key.b);
        inputProcessor.parseOperation(Key.b);
        edit = new CommandOperation(Key.a);
        inputProcessor.parseOperation(Key.a);
        edit = new CommandOperation(Key.r);
        inputProcessor.parseOperation(Key.r);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.SPACE);
        edit = new CommandOperation(Key.META_c);
        inputProcessor.parseOperation(Key.META_c);
        //line should be the same
        assertEquals("foo bar ", consoleBuffer.getBuffer().getLineNoMask());

        edit = new CommandOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.LEFT);
        edit = new CommandOperation(Key.META_c);
        inputProcessor.parseOperation(Key.META_c);
        assertEquals("foo Bar ", consoleBuffer.getBuffer().getLineNoMask());
        edit = new CommandOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.LEFT);

        edit = new CommandOperation(Key.META_c);
        inputProcessor.parseOperation(Key.META_c);
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
        inputProcessor.parseOperation(Key.F);
        edit = new CommandOperation(Key.o);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.o);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.SPACE);
        edit = new CommandOperation(Key.B);
        inputProcessor.parseOperation(Key.B);
        edit = new CommandOperation(Key.A);
        inputProcessor.parseOperation(Key.A);
        edit = new CommandOperation(Key.r);
        inputProcessor.parseOperation(Key.r);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.SPACE);
        edit = new CommandOperation(Key.META_l);
        inputProcessor.parseOperation(Key.META_l);
        //line should be the same
        assertEquals("Foo BAr ", consoleBuffer.getBuffer().getLineNoMask());

        edit = new CommandOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.LEFT);
        edit = new CommandOperation(Key.META_l);
        inputProcessor.parseOperation(Key.META_l);
        assertEquals("Foo bar ", consoleBuffer.getBuffer().getLineNoMask());
        edit = new CommandOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.LEFT);

        edit = new CommandOperation(Key.META_l);
        inputProcessor.parseOperation(Key.META_l);
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
        inputProcessor.parseOperation(Key.f);
        edit = new CommandOperation(Key.o);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.o);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.SPACE);
        edit = new CommandOperation(Key.b);
        inputProcessor.parseOperation(Key.b);
        edit = new CommandOperation(Key.a);
        inputProcessor.parseOperation(Key.a);
        edit = new CommandOperation(Key.r);
        inputProcessor.parseOperation(Key.r);
        edit = new CommandOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.SPACE);
        edit = new CommandOperation(Key.META_u);
        inputProcessor.parseOperation(Key.META_u);
        //line should be the same
        assertEquals("foo bar ", consoleBuffer.getBuffer().getLineNoMask());

        edit = new CommandOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.LEFT);
        edit = new CommandOperation(Key.META_u);
        inputProcessor.parseOperation(Key.META_u);
        assertEquals("foo BAR ", consoleBuffer.getBuffer().getLineNoMask());
        edit = new CommandOperation(Key.CTRL_A);
        inputProcessor.parseOperation(Key.CTRL_A);

        edit = new CommandOperation(Key.META_u);
        inputProcessor.parseOperation(Key.META_u);
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

        inputProcessor.parseOperation(Key.f);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.QUOTE);
        String result = inputProcessor.parseOperation(Key.ENTER);

        assertEquals(null, result);

        inputProcessor.parseOperation(Key.o);
        result = inputProcessor.parseOperation(Key.ENTER);
        assertEquals(null, result);

        inputProcessor.parseOperation(Key.QUOTE);
        result = inputProcessor.parseOperation(Key.ENTER);
        assertEquals("fo \"o\"", result);

    }

    @Test
    public void testMasking() throws IOException {
         ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
                .create();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder().shell(shell).prompt(new Prompt("aesh", '*')).create();

        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .settings(settings)
                .create();

        inputProcessor.parseOperation(Key.f);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.o);

        assertEquals("***", consoleBuffer.getBuffer().getLine());

        String result = inputProcessor.parseOperation(Key.ENTER);
        assertEquals("foo", result);

        inputProcessor.parseOperation(Key.f);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.BACKSPACE);
        inputProcessor.parseOperation(Key.BACKSPACE);
        inputProcessor.parseOperation(Key.ONE);
        inputProcessor.parseOperation(Key.TWO);

        assertEquals("***", consoleBuffer.getBuffer().getLine());
        result = inputProcessor.parseOperation(Key.ENTER);
        assertEquals("f12", result);

        //test with masking set to a null char
        consoleBuffer.setPrompt(new Prompt("aesh", '\u0000'));

        inputProcessor.parseOperation(Key.f);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.o);

        assertEquals("", consoleBuffer.getBuffer().getLine());
        assertEquals("foo", consoleBuffer.getBuffer().getLineNoMask());

        inputProcessor.parseOperation(Key.BACKSPACE);
        inputProcessor.parseOperation(Key.BACKSPACE);
        inputProcessor.parseOperation(Key.ONE);
        inputProcessor.parseOperation(Key.TWO);
        assertEquals("", consoleBuffer.getBuffer().getLine());

        result = inputProcessor.parseOperation(Key.ENTER);
        assertEquals("f12", result);
    }

}
