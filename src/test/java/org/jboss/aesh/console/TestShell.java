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

import java.io.PrintStream;

import org.jboss.aesh.console.reader.AeshStandardStream;
import org.jboss.aesh.terminal.CursorPosition;
import org.jboss.aesh.terminal.TerminalSize;
import org.jboss.aesh.terminal.api.Terminal;

public class TestShell implements Shell {

    private final PrintStream out;
    private final PrintStream err;

    public TestShell(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    @Override
    public Terminal getTerminal() {
        return null;
    }

    @Override
    public void clear() {
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
        return new TerminalSize(80, 20);
    }

    @Override
    public CursorPosition getCursor() {
        return new CursorPosition(1, 1);
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
