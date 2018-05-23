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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

/**
 * {@code ClassFileIterator} is used to iterate over all Java ClassFile files available within
 * a specific context.
 * <p>
 * For every Java ClassFile ({@code .class}) an {@link InputStream} is returned.
 *
 * @author <a href="mailto:rmuller@xiam.nl">Ronald K. Muller</a>
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public final class ClassFileIterator extends ResourceIterator {

    private final FileIterator fileIterator;
    private final String[] pkgNameFilter;
    private ZipFileIterator zipIterator;

    /**
     * Create a new {@code ClassFileIterator} returning all Java ClassFile files available
     * from the class path ({@code System.getProperty("java.class.path")}).
     */
    ClassFileIterator() {
        this(classPath(), null);
    }

    /**
     * Create a new {@code ClassFileIterator} returning all Java ClassFile files available
     * from the specified files and/or directories, including sub directories.
     * <p>
     * If the (optional) package filter is defined, only class files staring with one of the
     * defined package names are returned.
     * NOTE: package names must be defined in the native format (using '/' instead of '.').
     */
    public ClassFileIterator(final File[] filesOrDirectories, final String[] pkgNameFilter) {
        this.fileIterator = new FileIterator(filesOrDirectories);
        this.pkgNameFilter = pkgNameFilter;
    }

    /**
     * Return the name of the Java ClassFile returned from the last call to {@link #next()}.
     * The name is either the path name of a file or the name of an ZIP/JAR file entry.
     */
    public String getName() {
        // Both getPath() and getName() are very light weight method calls
        return zipIterator == null ?
                fileIterator.getFile().getPath() :
                zipIterator.getEntry().getName();
    }

    @Override
    public InputStream next() throws IOException {
        while (true) {
            if (zipIterator == null) {
                final File file = fileIterator.next();
                // not all specified Files exists!
                if (file == null || !file.isFile()) {
                    return null;
                } else {
                    final String name = file.getName();
                    if (name.endsWith(".class")) {
                        return new FileInputStream(file);
                    } else if (fileIterator.isRootFile() &&
                            (endsWithIgnoreCase(name, ".jar") || isZipFile(file))) {
                        zipIterator = new ZipFileIterator(new ZipFile(file), pkgNameFilter);
                    } // else just ignore
                }
            } else {
                final InputStream is = zipIterator.next();
                if (is == null) {
                    zipIterator = null;
                } else {
                    return is;
                }
            }
        }
    }

    // private

    private boolean isZipFile(final File file) {
        DataInputStream in = null;
        try {
            in = new DataInputStream(new FileInputStream(file));
            final int n = in.readInt();
            return n == 0x504b0304;
        } catch (IOException ex) {
            // silently ignore read exceptions
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }

    /**
     * Returns the class path of the current JVM instance as an array of {@link File} objects.
     */
    private static File[] classPath() {
        final String[] fileNames =
                System.getProperty("java.class.path").split(File.pathSeparator);
        final File[] files = new File[fileNames.length];
        for (int i = 0; i < files.length; ++i) {
            files[i] = new File(fileNames[i]);
        }
        return files;
    }

    private static boolean endsWithIgnoreCase(final String value, final String suffix) {
        final int n = suffix.length();
        return value.regionMatches(true, value.length() - n, suffix, 0, n);
    }

}
