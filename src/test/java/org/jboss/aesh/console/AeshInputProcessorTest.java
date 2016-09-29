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
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
                .create();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder()
                .shell(shell)
                .prompt(new Prompt("aesh"))
                .editMode(settings.editMode())
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
        inputProcessor.parseOperation(Key.h);
        inputProcessor.parseOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.s);
        inputProcessor.parseOperation(Key.RIGHT);
        inputProcessor.parseOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.r);
        inputProcessor.parseOperation(Key.u);
        inputProcessor.parseOperation(Key.l);
        inputProcessor.parseOperation(Key.e);

        inputProcessor.parseOperation(Key.CTRL_A);
        inputProcessor.parseOperation(Key.a);

        inputProcessor.parseOperation(Key.CTRL_E);

        inputProcessor.parseOperation(Key.s);
        String result = inputProcessor.parseOperation(Key.ENTER);

        assertEquals("aesh rules", result);
    }

    @Test
    public void testHistory() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
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

        inputProcessor.parseOperation(Key.f);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.b);
        inputProcessor.parseOperation(Key.a);
        inputProcessor.parseOperation(Key.r);

        String result = inputProcessor.parseOperation(Key.ENTER);

        assertEquals("foo bar", result);

        assertEquals("", consoleBuffer.getBuffer().getLine());

        inputProcessor.parseOperation(Key.UP);
        inputProcessor.parseOperation(Key.BACKSPACE);
        result = inputProcessor.parseOperation(Key.ENTER);
        assertEquals("foo ba", result);

        inputProcessor.parseOperation(Key.UP);
        inputProcessor.parseOperation(Key.DOWN);
        result = inputProcessor.parseOperation(Key.ENTER);
        assertEquals("", result);

    }

    @Test
    public void testUndo() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
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
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.b);
        inputProcessor.parseOperation(Key.a);
        inputProcessor.parseOperation(Key.r);

        inputProcessor.parseOperation(Key.BACKSPACE);
        inputProcessor.parseOperation(Key.BACKSPACE);
        inputProcessor.parseOperation(Key.BACKSPACE);

        inputProcessor.parseOperation(Key.CTRL_X_CTRL_U);
        inputProcessor.parseOperation(Key.CTRL_X_CTRL_U);

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
                .persistHistory(false)
                .historySize(10)
                .create();

        inputProcessor.parseOperation(Key.f);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.b);
        inputProcessor.parseOperation(Key.a);
        inputProcessor.parseOperation(Key.r);
        inputProcessor.parseOperation(Key.ONE);

        String result = inputProcessor.parseOperation(Key.ENTER);
        assertEquals("foo bar1", result);

        result = inputProcessor.parseOperation(Key.ENTER);
        assertEquals("", result);

        inputProcessor.parseOperation(Key.CTRL_R);
        inputProcessor.parseOperation(Key.f);
        result = inputProcessor.parseOperation(Key.ENTER);
        assertEquals("foo bar1", result);

        inputProcessor.parseOperation(Key.f);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.b);
        inputProcessor.parseOperation(Key.a);
        inputProcessor.parseOperation(Key.r);
        inputProcessor.parseOperation(Key.TWO);

        result = inputProcessor.parseOperation(Key.ENTER);
        assertEquals("foo bar2", result);

        inputProcessor.parseOperation(Key.CTRL_R);
        inputProcessor.parseOperation(Key.f);
        assertEquals("(reverse-i-search) `f': foo bar2", consoleBuffer.getBuffer().getLine());

        inputProcessor.parseOperation(Key.CTRL_R);
        assertEquals("(reverse-i-search) `f': foo bar1", consoleBuffer.getBuffer().getLine());
    }

    @Test
    public void testPrompt() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
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
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.b);
        inputProcessor.parseOperation(Key.a);
        inputProcessor.parseOperation(Key.r);
        inputProcessor.parseOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.META_c);
        //line should be the same
        assertEquals("foo bar ", consoleBuffer.getBuffer().getLineNoMask());

        inputProcessor.parseOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.META_c);
        assertEquals("foo Bar ", consoleBuffer.getBuffer().getLineNoMask());
        inputProcessor.parseOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.LEFT);

        inputProcessor.parseOperation(Key.META_c);
        assertEquals("Foo Bar ", consoleBuffer.getBuffer().getLineNoMask());
    }

    @Test
    public void testLowerCaseWord() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
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

        inputProcessor.parseOperation(Key.F);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.B);
        inputProcessor.parseOperation(Key.A);
        inputProcessor.parseOperation(Key.r);
        inputProcessor.parseOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.META_l);
        //line should be the same
        assertEquals("Foo BAr ", consoleBuffer.getBuffer().getLineNoMask());

        inputProcessor.parseOperation(Key.META_b);
        inputProcessor.parseOperation(Key.META_l);
        assertEquals("Foo bar ", consoleBuffer.getBuffer().getLineNoMask());
        inputProcessor.parseOperation(Key.META_b);
        inputProcessor.parseOperation(Key.META_b);
        inputProcessor.parseOperation(Key.META_l);
        assertEquals("foo bar ", consoleBuffer.getBuffer().getLineNoMask());
    }

    @Test
    public void testUpperCaseWord() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
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
        inputProcessor.parseOperation(Key.o);
        inputProcessor.parseOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.b);
        inputProcessor.parseOperation(Key.a);
        inputProcessor.parseOperation(Key.r);
        inputProcessor.parseOperation(Key.SPACE);
        inputProcessor.parseOperation(Key.META_u);
        //line should be the same
        assertEquals("foo bar ", consoleBuffer.getBuffer().getLineNoMask());

        inputProcessor.parseOperation(Key.META_b);
        inputProcessor.parseOperation(Key.META_u);
        assertEquals("foo BAR ", consoleBuffer.getBuffer().getLineNoMask());
        inputProcessor.parseOperation(Key.META_b);
        inputProcessor.parseOperation(Key.META_b);
        inputProcessor.parseOperation(Key.META_u);
        assertEquals("FOO BAR ", consoleBuffer.getBuffer().getLineNoMask());
    }

    @Test
    public void testQuotes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
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
