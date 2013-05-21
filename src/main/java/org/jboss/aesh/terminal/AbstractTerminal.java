/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.util.ANSI;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public abstract class AbstractTerminal implements Terminal {

    private Logger logger;

    AbstractTerminal(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void writeChar(TerminalCharacter character) throws IOException {
        writeToStdOut(character.getAsString());
    }

    @Override
    public void writeChars(List<TerminalCharacter> chars) throws IOException {
        StringBuilder builder = new StringBuilder();
        TerminalCharacter prev = null;
        for(TerminalCharacter c : chars) {
            if(prev == null)
                builder.append(c.getAsString());
            else
                builder.append(c.getAsString(prev));
            prev = c;
        }
        writeToStdOut(builder.toString());
    }

    @Override
    public void writeString(TerminalString termString) throws IOException {
        writeToStdOut(termString.getAsString());
    }

    /**
     * Return the row position if we use a ansi terminal
     * Send a terminal: '<ESC>[6n'
     * and we receive the position as: '<ESC>[n;mR'
     * where n = current row and m = current column
     */
    @Override
    public CursorPosition getCursor() {
        if(Settings.getInstance().isAnsiConsole() && Config.isOSPOSIXCompatible()) {
            try {
                writeToStdOut(ANSI.getCurrentCursorPos());
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
                if(Settings.getInstance().isLogging())
                    logger.log(Level.SEVERE, "Failed to find current row with ansi code: ",e);
                return new CursorPosition(-1,-1);
            }
        }
        return new CursorPosition(-1,-1);
    }

    @Override
    public void setCursor(CursorPosition position) throws IOException {
        if(getSize().isPositionWithinSize(position)) {
            writeToStdOut(position.asAnsi());
        }
    }

    @Override
    public void moveCursor(int rows, int columns) throws IOException {
        CursorPosition cp = getCursor();
        cp.move(rows, columns);
        if(getSize().isPositionWithinSize(cp)) {
            setCursor(cp);
        }
    }

    @Override
    public void clear() throws IOException {
        writeToStdOut(ANSI.clearScreen());
    }

}
