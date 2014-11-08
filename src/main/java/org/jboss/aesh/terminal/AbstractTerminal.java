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

import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.reader.AeshStandardStream;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.util.ANSI;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public abstract class AbstractTerminal implements Terminal, Shell {

    private final Logger logger;
    protected Settings settings;
    private boolean mainBuffer = true;

    AbstractTerminal(Logger logger) {
        this.logger = logger;
    }


    /**
     * Return the row position if we use a ansi terminal
     * Send a terminal: '<ESC>[6n'
     * and we receive the position as: '<ESC>[n;mR'
     * where n = current row and m = current column
     */
    @Override
    public CursorPosition getCursor() {
        if(settings.isAnsiConsole() && Config.isOSPOSIXCompatible()) {
            try {
                StringBuilder col = new StringBuilder(4);
                StringBuilder row = new StringBuilder(4);
                out().print(ANSI.CURSOR_ROW);
                out().flush();
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
                    logger.log(Level.SEVERE, "Failed to find current row with ansi code: ",e);
                return new CursorPosition(-1,-1);
            }
        }
        return new CursorPosition(-1,-1);
    }

    @Override
    public void setCursor(CursorPosition position) {
        if(getSize().isPositionWithinSize(position)) {
            out().print(position.asAnsi());
            out().flush();
        }
    }

    @Override
    public void moveCursor(int rows, int columns) {
        CursorPosition cp = getCursor();
        cp.move(rows, columns);
        if(getSize().isPositionWithinSize(cp)) {
            setCursor(cp);
        }
    }

    @Override
    public void clear() {
        out().print(ANSI.CLEAR_SCREEN);
        out().flush();
    }

    @Override
    public boolean isMainBuffer() {
        return mainBuffer;
    }

    @Override
    public void enableAlternateBuffer() {
        if(isMainBuffer()) {
            out().print(ANSI.ALTERNATE_BUFFER);
            out().flush();
            mainBuffer = false;
        }
    }

    @Override
    public void enableMainBuffer() {
        if(!isMainBuffer()) {
            out().print(ANSI.MAIN_BUFFER);
            out().flush();
            mainBuffer = true;
        }
    }

    @Override
    public Shell getShell() {
        return this;
    }

    @Override
    public AeshStandardStream in() {
        return null;
    }
}
