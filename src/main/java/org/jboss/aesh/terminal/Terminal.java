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

import org.jboss.aesh.console.settings.Settings;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;

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
    int[] read() throws IOException;

    /**
     * Check if the terminal has input waiting to be read.
     *
     * @return
     */
    boolean hasInput();

    boolean isEchoEnabled();

    /**
     * Set it back to normal when we exit
     *
     * @throws java.io.IOException stream
     */
    void reset() throws IOException;

    Shell getShell();

    void writeToInputStream(String data);

    /**
     * During runtime, change the output stream
     * @param output stream
     */
    void changeOutputStream(PrintStream output);
}
