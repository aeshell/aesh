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

import static org.junit.Assert.assertSame;

import java.util.function.Consumer;

import org.aesh.terminal.tty.Signal;
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
}
