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
package org.aesh.console;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.aesh.command.shell.Shell;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.tty.Signal;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

public class HandlerScopeTest {

    @Test
    public void testStdinNestingRestoresInOrder() {
        TestConnection connection = new TestConnection();
        Consumer<int[]> initial = connection.stdinHandler();
        Consumer<int[]> outer = ints -> {
        };
        Consumer<int[]> inner = ints -> {
        };

        HandlerScope outerScope = HandlerScope.stdin(connection, outer);
        assertSame(outer, connection.stdinHandler());
        HandlerScope innerScope = HandlerScope.stdin(connection, inner);
        assertSame(inner, connection.stdinHandler());
        innerScope.close();
        assertSame(outer, connection.stdinHandler());
        innerScope.close();
        assertSame(outer, connection.stdinHandler());
        outerScope.close();
        assertSame(initial, connection.stdinHandler());
    }

    @Test
    public void testSignalNestingRestoresInOrder() {
        TestConnection connection = new TestConnection();
        Consumer<Signal> initial = connection.signalHandler();
        Consumer<Signal> outer = signal -> {
        };
        Consumer<Signal> inner = signal -> {
        };

        HandlerScope outerScope = HandlerScope.signal(connection, outer);
        assertSame(outer, connection.signalHandler());
        HandlerScope innerScope = HandlerScope.signal(connection, inner);
        assertSame(inner, connection.signalHandler());
        innerScope.close();
        assertSame(outer, connection.signalHandler());
        outerScope.close();
        assertSame(initial, connection.signalHandler());
    }

    @Test
    public void testStdinLeaveCurrentOnClosePreservesSuccessor() {
        // A deliberate handoff installed before close (readline's
        // between-cycle buffering handler) must survive the scope.
        TestConnection connection = new TestConnection();
        Consumer<int[]> initial = ints -> {
        };
        connection.setStdinHandler(initial);
        Consumer<int[]> leased = ints -> {
        };
        HandlerScope scope = HandlerScope.stdinLeaveCurrentOnClose(connection, leased);
        assertSame(leased, connection.stdinHandler());
        Consumer<int[]> successor = ints -> {
        };
        connection.setStdinHandler(successor);
        scope.close();
        assertSame(successor, connection.stdinHandler());
    }

    @Test
    public void testStdinScopeStillRestoresSaved() {
        // The default mode is unchanged: close restores the captured handler.
        TestConnection connection = new TestConnection();
        Consumer<int[]> initial = connection.stdinHandler();
        Consumer<int[]> leased = ints -> {
        };
        HandlerScope scope = HandlerScope.stdin(connection, leased);
        assertSame(leased, connection.stdinHandler());
        scope.close();
        assertSame(initial, connection.stdinHandler());
    }

    @Test
    public void testReadLineKeepsBetweenCycleHandlerWithoutWarning() throws Exception {
        // ShellImpl.readLine() must not log the lease out-of-order warning:
        // readline installs its between-cycle buffering handler before the
        // read completes, and the scope must leave that handoff installed.
        TestConnection connection = new TestConnection();
        connection.openBlocking();
        Consumer<int[]> initial = ints -> {
        };
        connection.setStdinHandler(initial);
        Shell shell = new ShellImpl(connection);

        Logger logger = Logger.getLogger("org.aesh.terminal.StdinLeaseImpl");
        List<LogRecord> records = new CopyOnWriteArrayList<>();
        Handler capture = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Level previousLevel = logger.getLevel();
        logger.addHandler(capture);
        logger.setLevel(Level.ALL);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> line = executor.submit(() -> shell.readLine(new Prompt("")));
            long deadline = System.currentTimeMillis() + 5000;
            while (connection.stdinHandler() == initial && System.currentTimeMillis() < deadline)
                Thread.sleep(10);
            connection.read("hello" + Config.getLineSeparator());
            assertTrue("readLine returned the typed line",
                    "hello".equals(line.get(10, TimeUnit.SECONDS)));
            assertNotNull("a handler must be installed after readLine",
                    connection.stdinHandler());
            assertNotSame("the between-cycle handler must survive, not the captured one",
                    initial, connection.stdinHandler());
            assertTrue("no lease warning must be logged, got: " + records,
                    records.stream().noneMatch(record -> String.valueOf(record.getMessage())
                            .contains("closed out of order")));
        } finally {
            executor.shutdownNow();
            logger.removeHandler(capture);
            logger.setLevel(previousLevel);
        }
    }
}
