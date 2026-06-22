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
import java.io.FileNotFoundException;
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

    private final String path;
    private BufferedInputStream cachedStream;

    InputDelegate(String path) {
        this.path = path;
    }

    /**
     * Returns the input stream for the redirected file.
     * Opens the file on first call, returns the cached stream on subsequent calls.
     *
     * @return the input stream, or null if the file does not exist
     */
    public BufferedInputStream read() {
        if (cachedStream != null)
            return cachedStream;
        try {
            cachedStream = new BufferedInputStream(new FileInputStream(new File(path)));
            return cachedStream;
        } catch (FileNotFoundException e) {
            return null;
        }
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
