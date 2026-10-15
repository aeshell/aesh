/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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
package org.aesh.command.completer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.aesh.command.impl.completer.AeshCompletionHandler;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.DefaultAeshContext;
import org.aesh.console.ReadlineConsole;
import org.aesh.readline.Buffer;
import org.aesh.readline.InputProcessor;
import org.aesh.readline.cursor.CursorListener;
import org.aesh.readline.cursor.Line;
import org.aesh.terminal.Key;
import org.aesh.terminal.formatting.TerminalString;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Console rendering hooks (#679): a per-keystroke cursor listener for
 * live highlighting, and a replaceable completion handler. The cursor
 * hook fires through a real read; the completion hook is proven by
 * subclassing {@code AeshCompletionHandler} and overriding only the
 * list-display seam.
 */
public class ConsoleRenderHooksTest {

    @Test
    public void testHookSettingsRoundTrip() {
        CursorListener listener = new CursorListener() {
            @Override
            public void moved(Line line) {
            }
        };
        AeshCompletionHandler handler = new AeshCompletionHandler(new DefaultAeshContext());

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .cursorListener(listener)
                .completionHandler(handler)
                .build();
        assertEquals(listener, settings.cursorListener());
        assertEquals(handler, settings.completionHandler());

        Settings<CommandInvocation> copy = new SettingsBuilder<CommandInvocation>(settings).build();
        assertEquals(listener, copy.cursorListener());
        assertEquals(handler, copy.completionHandler());

        Settings<CommandInvocation> defaults = SettingsBuilder.builder().build();
        assertNull(defaults.cursorListener());
        assertNull(defaults.completionHandler());
    }

    @Test
    public void testCursorListenerFiresOnMovement() throws Exception {
        TestConnection con = new TestConnection();
        final AtomicBoolean fired = new AtomicBoolean(false);
        Settings<CommandInvocation> settings = SettingsBuilder
                .builder()
                .connection(con)
                .cursorListener(new CursorListener() {
                    @Override
                    public void moved(Line line) {
                        fired.set(true);
                    }
                })
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();
        try {
            // Plain insertion does not move the cursor; an arrow key does.
            con.read("ab");
            con.read(Key.LEFT_2);
            long deadline = System.currentTimeMillis() + 5000;
            while (!fired.get() && System.currentTimeMillis() < deadline)
                Thread.sleep(25);
            assertTrue("cursor listener must fire on cursor movement", fired.get());
        } finally {
            console.stop();
        }
    }

    private static class RecordingDisplayHandler extends AeshCompletionHandler {
        final AtomicBoolean completeCalled = new AtomicBoolean(false);
        final AtomicInteger displayCalls = new AtomicInteger(0);

        RecordingDisplayHandler() {
            super(new DefaultAeshContext());
        }

        @Override
        public void complete(InputProcessor inputProcessor) {
            completeCalled.set(true);
            super.complete(inputProcessor);
        }

        @Override
        protected void displayCompletions(List<TerminalString> completions, Buffer buffer,
                InputProcessor inputProcessor) {
            displayCalls.incrementAndGet();
            super.displayCompletions(completions, buffer, inputProcessor);
        }
    }

    @Test
    public void testCustomCompletionHandlerDisplaySeam() throws Exception {
        TestConnection con = new TestConnection();
        RecordingDisplayHandler handler = new RecordingDisplayHandler();
        Settings<CommandInvocation> settings = SettingsBuilder
                .builder()
                .connection(con)
                .completionHandler(handler)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.addCompletion(co -> {
            co.addCompletionCandidate("alpha");
            co.addCompletionCandidate("beta");
        });
        console.start();
        try {
            con.read(Key.CTRL_I);
            long deadline = System.currentTimeMillis() + 5000;
            while (handler.displayCalls.get() == 0 && System.currentTimeMillis() < deadline)
                Thread.sleep(25);
            assertTrue("custom handler must run on Tab", handler.completeCalled.get());
            assertTrue("list display seam must route through the override",
                    handler.displayCalls.get() > 0);
            assertNotNull(con.getOutputBuffer());
        } finally {
            console.stop();
        }
    }
}
