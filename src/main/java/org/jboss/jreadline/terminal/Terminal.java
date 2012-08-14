/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.jreadline.terminal;

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
     * @return terminal height
     */
    int getHeight();

    /**
     * @return terminal width
     */
    int getWidth();

    boolean isEchoEnabled();

    /**
     * Set it back to normal when we exit
     *
     * @throws java.io.IOException stream
     */
    void reset() throws IOException;


}
