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
package org.jboss.aesh.terminal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.jboss.aesh.console.reader.AeshInputStream;
import org.jboss.aesh.console.reader.AeshStandardStream;
import org.jboss.aesh.console.settings.Settings;

/**
 * A dummy terminal used for tests
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class TestTerminal implements Terminal, Shell {

    private PrintStream outWriter;
    private PrintStream errWriter;
    private TerminalSize size;
    private AeshInputStream input;
    private PrintStream stdOut;
    private PrintStream stdErr;

    @Override
    public void init(Settings settings) {
        input =  new AeshInputStream(settings.getInputStream());
        outWriter = new PrintStream(settings.getStdOut(), true);
        errWriter = new PrintStream(settings.getStdErr(), true);
        this.stdOut = settings.getStdOut();
        this.stdErr = settings.getStdErr();

        size = new TerminalSize(24,80);
    }

    @Override
    public int[] read() throws IOException {
            return input.readAll();
    }

    @Override
    public TerminalSize getSize() {
        return size;
    }

    @Override
    public CursorPosition getCursor() {
        return new CursorPosition(0,0);
    }

    @Override
    public void setCursor(CursorPosition cp) {
    }

    @Override
    public void moveCursor(int r, int c) {
    }

    @Override
    public boolean isMainBuffer() {
        return true;
    }

    @Override
    public void enableAlternateBuffer() {
        //do nothing
    }

    @Override
    public void enableMainBuffer() {
        //do nothing
    }

    @Override
    public boolean isEchoEnabled() {
        return false;
    }

    @Override
    public void reset() throws IOException {
    }

    @Override
    public void close() throws IOException {
        input.stop();
    }

    @Override
    public Shell getShell() {
        return this;
    }

    @Override
    public AeshInputStream getInputStream() {
        return input;
    }

    @Override
    public void clear() throws IOException {
    }

    @Override
    public PrintStream err() {
        return errWriter;
    }

    @Override
    public AeshStandardStream in() {
        return new AeshStandardStream(new BufferedInputStream(input));
    }

    @Override
    public PrintStream out() {
        return outWriter;
    }

}
