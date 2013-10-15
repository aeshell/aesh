/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import org.jboss.aesh.console.reader.AeshPrintStream;

import java.io.IOException;

/**
 * A Shell is an abstraction of the Terminal that provides easy to use methods
 * to manipulate text/cursor/buffers.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface Shell {

    /**
     * clears the screen
     */
    void clear() throws IOException;

    /**
     * Returns the {@link AeshPrintStream} associated with the std out
     */
    AeshPrintStream out();

    /**
     * Returns the {@link AeshPrintStream} associated with the std err
     */
    AeshPrintStream err();

    /**
     * @return terminal size
     */
    TerminalSize getSize();

    /**
     * @return get the cursor position
     */
    CursorPosition getCursor();

    /**
     * Set cursor position
     */
    void setCursor(CursorPosition position) throws IOException;

    /**
     * Move the cursor relative to the current position
     * Will not move outside of TerminalSize boundaries
     */
    void moveCursor(int rows, int columns) throws IOException;

    /**
     * @return true if current buffer is main
     */
    boolean isMainBuffer();

    /**
     * If not alternate buffer is enabled this will enable it
     *
     * @throws IOException
     */
    void enableAlternateBuffer() throws IOException;

    /**
     * If not main buffer is enabled this will enable it.
     * @throws IOException
     */
    void enableMainBuffer() throws IOException;

}
