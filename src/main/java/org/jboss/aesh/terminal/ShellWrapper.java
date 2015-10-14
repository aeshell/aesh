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

import java.io.IOError;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.aesh.console.reader.AeshStandardStream;
import org.jboss.aesh.console.reader.ConsoleInputSession;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.terminal.api.Attributes;
import org.jboss.aesh.terminal.api.Console;
import org.jboss.aesh.terminal.api.Console.Signal;
import org.jboss.aesh.terminal.api.ConsoleBuilder;
import org.jboss.aesh.terminal.api.Size;
import org.jboss.aesh.terminal.utils.InfoCmp.Capability;
import org.jboss.aesh.util.LoggerUtil;

public class ShellWrapper implements Terminal, Shell {

    private static final Logger LOGGER = LoggerUtil.getLogger(ShellWrapper.class.getName());

    private Settings settings;
    private Console console;
    private PrintStream out;
    private PrintStream err;
    private ConsoleInputSession input;
    private Attributes attributes;

    private boolean mainBuffer = true;

    public ShellWrapper(Settings settings) {
        this.settings = settings;
    }

    @Override
    public void init(Settings settings) {
        try {
            console = ConsoleBuilder.builder()
                    .streams(settings.getInputStream(), settings.getStdOut())
                    .name("Aesh console")
                    .build();
            attributes = console.enterRawMode();
            out = new PrintStream(console.output());
            err = settings.getStdErr();
            input = new ConsoleInputSession(console.input());
            // bridge to the current way of supporting signals
            console.handle(Signal.INT, s -> input.writeToInput("\u0003"));
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public Console getConsole() {
        return console;
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
    public TerminalSize getSize() {
        Size size = console.getSize();
        return new TerminalSize(size.getHeight(), size.getWidth());
    }

    @Override
    public int[] read() throws IOException {
        return input.readAll();
    }

    @Override
    public boolean hasInput() {
        return input.hasInput();
    }

    @Override
    public boolean isEchoEnabled() {
        return console.echo();
    }

    @Override
    public void reset() throws IOException {
        console.setAttributes(attributes);
    }

    @Override
    public void writeToInputStream(String data) {
        input.writeToInput(data);
    }

    @Override
    public void changeOutputStream(PrintStream output) {
        out = output;
    }

    @Override
    public void close() throws IOException {
        console.close();
    }

    @Override
    public Shell getShell() {
        return this;
    }

    @Override
    public void clear() throws IOException {
        console.puts(Capability.clear_screen);
    }

    @Override
    public AeshStandardStream in() {
        return null;
    }

    @Override
    public CursorPosition getCursor() {
        if (console.puts(Capability.user7)) {
            try {
                console.flush();
                StringBuilder col = new StringBuilder(4);
                StringBuilder row = new StringBuilder(4);
                boolean gotSep = false;
                //read the position
                int[] input = read();

                for(int i=2; i < input.length-1; i++) {
                    if(input[i] == 59) // we got a ';' which is the separator
                        gotSep = true;
                    else {
                        if(gotSep)
                            col.append((char) input[i]);
                        else
                            row.append((char) input[i]);
                    }
                }
                return new CursorPosition(Integer.parseInt(row.toString()), Integer.parseInt(col.toString()));
            }
            catch (Exception e) {
                if(settings.isLogging())
                    LOGGER.log(Level.SEVERE, "Failed to find current row with ansi code: ",e);
                return new CursorPosition(-1,-1);
            }
        }
        return new CursorPosition(-1,-1);
    }

    @Override
    public void setCursor(CursorPosition position) {
        if (getSize().isPositionWithinSize(position)
                && console.puts(Capability.cursor_address,
                                position.getRow(),
                                position.getColumn())) {
            console.flush();
        }
    }

    @Override
    public void moveCursor(int rows, int columns) {
        CursorPosition cp = getCursor();
        cp.move(rows, columns);
        if (getSize().isPositionWithinSize(cp)) {
            setCursor(cp);
        }
    }

    @Override
    public boolean isMainBuffer() {
        return mainBuffer;
    }

    @Override
    public void enableAlternateBuffer() {
        if (isMainBuffer() && console.puts(Capability.enter_ca_mode)) {
            console.flush();
            mainBuffer = false;
        }
    }

    @Override
    public void enableMainBuffer() {
        if (!isMainBuffer() && console.puts(Capability.exit_ca_mode)) {
            console.flush();
            mainBuffer = false;
        }
    }
}
