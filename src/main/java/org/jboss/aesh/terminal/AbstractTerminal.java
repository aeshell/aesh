/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    private Logger logger;
    protected final Settings settings;
    private boolean mainBuffer = true;

    AbstractTerminal(Settings settings, Logger logger) {
        this.settings = settings;
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
                out().print(ANSI.getCurrentCursorPos());
                out().flush();
                StringBuilder builder = new StringBuilder(8);
                int row;
                while((row = read(false)[0]) > -1 && row != 'R') {
                    if (row != 27 && row != '[') {
                        builder.append((char) row);
                    }
                }
                return new CursorPosition(Integer.parseInt(builder.substring(0, builder.indexOf(";"))),
                        Integer.parseInt(builder.substring(builder.lastIndexOf(";") + 1, builder.length())));
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
        out().print(ANSI.clearScreen());
        out().flush();
    }

    @Override
    public boolean isMainBuffer() {
        return mainBuffer;
    }

    @Override
    public void enableAlternateBuffer() {
        if(isMainBuffer()) {
            out().print(ANSI.getAlternateBufferScreen());
            out().flush();
            mainBuffer = false;
        }
    }

    @Override
    public void enableMainBuffer() {
        if(!isMainBuffer()) {
            out().print(ANSI.getMainBufferScreen());
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
