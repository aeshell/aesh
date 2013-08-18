/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import org.jboss.aesh.console.reader.AeshPrintWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Generic interface for Terminals
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public interface Terminal {

    /**
     * Initialize the Terminal with which input/output stream it should use
     *
     * @param inputStream input
     * @param stdOut standard output
     * @param stdErr error output
     */
    void init(InputStream inputStream, OutputStream stdOut, OutputStream stdErr);

    /**
     * Read from the input stream (char by char)
     *
     * @return whats read
     * @throws IOException
     */
    int[] read(boolean readAhead) throws IOException;

    /**
     * Write to the standard output stream
     *
     * @param out what goes into the stream
     * @throws IOException stream
     */
    void writeToStdOut(String out) throws IOException;

    /**
     * Write to the standard output stream
     *
     * @param out what goes into the stream
     * @throws IOException stream
     */
    void writeToStdOut(char[] out) throws IOException;

    /**
     * Write to the standard output stream
     *
     * @param out what goes into the stream
     * @throws IOException stream
     */
    void writeToStdOut(char out) throws IOException;

    /**
     * Write a TerminalCharacter to std out
     */
    void writeToStdOut(TerminalCharacter c) throws IOException;

    /**
     * Write a list of TerminalCharacters to std out
     */
    void writeToStdOut(List<TerminalCharacter> chars) throws IOException;

    /**
     * Write a TerminalString to std out
     */
    void writeStdOut(TerminalString string) throws IOException;

    /**
     * Write to the standard error stream
     *
     * @param err what goes into the stream
     * @throws IOException stream
     */
    void writeToStdErr(String err) throws IOException;

    /**
     * Write to the standard error stream
     *
     * @param err what goes into the stream
     * @throws IOException stream
     */
    void writeToStdErr(char[] err) throws IOException;

    /**
     * Write to the standard error stream
     *
     * @param err what goes into the stream
     * @throws IOException stream
     */
    void writeToStdErr(char err) throws IOException;

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

    boolean isEchoEnabled();

    /**
     * Set it back to normal when we exit
     *
     * @throws java.io.IOException stream
     */
    void reset() throws IOException;

    /**
     * clears the screen
     */
    void clear() throws IOException;

    /**
     * Returns the {@link AeshPrintWriter} associated with the std out
     */
    AeshPrintWriter getStdOut();

    /**
     * Returns the {@link AeshPrintWriter} associated with the std err
     */
    AeshPrintWriter getStdErr();
}
