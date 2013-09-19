/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

    boolean isEchoEnabled();

    /**
     * Set it back to normal when we exit
     *
     * @throws java.io.IOException stream
     */
    void reset() throws IOException;

    Shell getShell();

}
