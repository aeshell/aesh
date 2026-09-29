/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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
package org.aesh.command.impl.operator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Manages input redirection from a file.
 * <p>
 * The stream is opened lazily on first {@link #read()} call and cached
 * for subsequent calls. Call {@link #close()} to release the underlying
 * file handle.
 *
 * @author Aesh team
 */
public class InputDelegate {

    private final File file;
    private BufferedInputStream cachedStream;

    InputDelegate(File file) {
        if (file == null)
            throw new NullPointerException("input redirection requires a file");
        this.file = file;
    }

    /**
     * The resolved file this delegate reads, for diagnostics.
     *
     * @return the input file
     */
    public File file() {
        return file;
    }

    /**
     * Opens the input, failing fast on missing or unreadable files. The
     * opened stream is cached, so a later {@link #read()} reuses it.
     *
     * @throws IOException when the file cannot be opened
     */
    void verify() throws IOException {
        readOrThrow();
    }

    /**
     * Returns the input stream for the redirected file.
     * Opens the file on first call, returns the cached stream on subsequent calls.
     *
     * @return the input stream, or null if the file cannot be opened
     *         (e.g. removed between verification and first read)
     */
    public BufferedInputStream read() {
        try {
            return readOrThrow();
        } catch (IOException e) {
            return null;
        }
    }

    private BufferedInputStream readOrThrow() throws IOException {
        if (cachedStream == null)
            cachedStream = new BufferedInputStream(new FileInputStream(file));
        return cachedStream;
    }

    /**
     * Close the underlying stream if open.
     */
    public void close() {
        if (cachedStream != null) {
            try {
                cachedStream.close();
            } catch (IOException ignored) {
            }
            cachedStream = null;
        }
    }
}
