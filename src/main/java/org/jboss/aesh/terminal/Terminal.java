/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import org.jboss.aesh.console.settings.Settings;

import java.io.Closeable;
import java.io.IOException;

/**
 * Generic interface for Terminals
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public interface Terminal extends Closeable {

    /**
     * Initialize the Terminal with which input/output stream it should use
     */
    void init(Settings settings);

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
