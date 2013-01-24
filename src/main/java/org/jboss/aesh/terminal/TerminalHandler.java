/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import org.jboss.aesh.util.ANSI;

import java.io.IOException;

/**
 * Helper class to write to a terminal.
 * Used for graphical/curses programs/commands.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TerminalHandler {

    private Terminal terminal;
    private CursorPosition position;
    private Color backgroundColor;
    private Color foregroundColor;


    public TerminalHandler(final Terminal terminal) {
        this.terminal = terminal;
        position = new CursorPosition(0,0);
        backgroundColor = Color.DEFAULT;
        foregroundColor = Color.DEFAULT;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public void clearScreen() throws IOException {
        terminal.writeToStdOut(ANSI.clearScreen());

    }

    public void enableAlternateScreen() throws IOException {
        terminal.writeToStdOut(ANSI.getAlternateBufferScreen());
    }

    public void enableMainScreen() throws IOException {
        terminal.writeToStdOut(ANSI.getMainBufferScreen());
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public CursorPosition getPosition() {
        return position;
    }

    public void setPosition(CursorPosition position) {
        this.position = position;
    }

    public Color getForegroundColor() {
        return foregroundColor;
    }

    public void setForegroundColor(Color foregroundColor) {
        this.foregroundColor = foregroundColor;
    }
}
