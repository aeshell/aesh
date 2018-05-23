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
package org.aesh.io.scanner;

import java.io.File;
import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * {@code FileIterator} enables iteration over all files in a directory and all its sub
 * directories.
 * <p>
 * Usage:
 * <pre>
 * FileIterator iter = new FileIterator(new File("./src"));
 * File f;
 * while ((f = iter.next()) != null) {
 *     // do something with f
 *     assert f == iter.getCurrent();
 * }
 * </pre>
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
final class FileIterator {

    private final Deque<File> stack = new LinkedList<File>();
    private int rootCount;
    private File current;

    /**
     * Create a new {@code FileIterator} using the specified 'filesOrDirectories' as root.
     * <p>
     * If 'filesOrDirectories' contains a file, the iterator just returns that single file.
     * If 'filesOrDirectories' contains a directory, all files in that directory
     * and its sub directories are returned (depth first).
     *
     * @param filesOrDirectories Zero or more {@link File} objects, which are iterated
     * in the specified order (depth first)
     */
    FileIterator(final File... filesOrDirectories) {
        addReverse(filesOrDirectories);
        rootCount = stack.size();
    }

    /**
     * Return the last returned file or {@code null} if no more files are available.
     *
     * @see #next()
     */
    public File getFile() {
        return current;
    }

    /**
     * Return {@code true} if the current file is one of the files originally
     * specified as one of the constructor file parameters, i.e. is a root file
     * or directory.
     */
    public boolean isRootFile() {
        if (current == null) {
            throw new NoSuchElementException();
        }
        return stack.size() < rootCount;
    }

    /**
     * Return the next {@link File} object or {@code null} if no more files are
     * available.
     *
     * @see #getFile()
     */
    public File next() throws IOException {
        if (stack.isEmpty()) {
            current = null;
            return null;
        } else {
            current = stack.removeLast();
            if (current.isDirectory()) {
                if (stack.size() < rootCount) {
                    rootCount = stack.size();
                }
                addReverse(current.listFiles());
                return next();
            } else {
                return current;
            }
        }
    }

    /**
     * Add the specified files in reverse order.
     */
    private void addReverse(final File[] files) {
        for (int i = files.length - 1; i >= 0; --i) {
            stack.add(files[i]);
        }
    }

}