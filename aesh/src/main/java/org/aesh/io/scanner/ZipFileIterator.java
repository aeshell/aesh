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

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * {@code ZipFileIterator} is used to iterate over all entries in a given {@code zip} or
 * {@code jar} file and returning the {@link InputStream} of these entries.
 * <p>
 * It is possible to specify an (optional) entry name filter.
 * <p>
 * The most efficient way of iterating is used, see benchmark in test classes.
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 *
 */
final class ZipFileIterator {

    private final ZipFile zipFile;
    private final String[] entryNameFilter;
    private final Enumeration<? extends ZipEntry> entries;

    private ZipEntry current;

    /**
     * Create a new {@code ZipFileIterator} instance.
     *
     * @param zipFile The ZIP file used to iterate over all entries
     * @param entryNameFilter (optional) file name filter. Only entry names starting with
     * one of the specified names in the filter are returned
     */
    ZipFileIterator(final ZipFile zipFile, final String[] entryNameFilter) throws IOException {
        this.zipFile = zipFile;
        this.entryNameFilter = entryNameFilter;

        this.entries = zipFile.entries();
    }

    public ZipEntry getEntry() {
        return current;
    }

    @SuppressWarnings("emptyblock")
    public InputStream next() throws IOException {
        while (entries.hasMoreElements()) {
            current = entries.nextElement();
            if (accept(current)) {
                return zipFile.getInputStream(current);
            }
        }
        // no more entries in this ZipFile, so close ZipFile
        try {
            // zipFile is never null here
            zipFile.close();
        } catch (IOException ex) {
            // suppress IOException, otherwise close() is called twice
        }
        return null;
    }

    private boolean accept(final ZipEntry entry) {
        if (entry.isDirectory()) {
            return false;
        }
        if (entryNameFilter == null) {
            return true;
        }
        for (final String filter : entryNameFilter) {
            if (entry.getName().startsWith(filter)) {
                return true;
            }
        }
        return false;
    }

}
