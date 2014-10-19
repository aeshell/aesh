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
package org.jboss.aesh.io;

import org.jboss.aesh.io.filter.ResourceFilter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**
 * FileResource is a Aesh native io class.
 * The default implementation is based on java.io.File, but it can be specified
 * by the Settings object when it needs to be overridden.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface Resource {

    /**
     * Returns the name of this file resource
     *
     * @return name
     */
    String getName();

    /**
     * Returns the absolute file resource string of this
     *
     * @return absolute path
     */
    String getAbsolutePath();

    /**
     * Return true if this file resource is a file, not a directory.
     * exists() must return true for isLeaf() to return true.
     *
     * @return true if file
     */
    boolean isLeaf();

    /**
     * Check if the file denoted is a directory.
     *
     * @return true if directory
     */
    boolean isDirectory();

    /**
     * Return true if this resource is a symbolic link.
     *
     * @return true if symbolic link
     */
    boolean isSymbolicLink();

    /**
     * If the Resource system support symbolic links and
     * this is a symbolic link, return the link target.
     * The target does not need to exist.
     *
     * @return link target
     */
    Resource readSymbolicLink() throws IOException;

    /**
     * Check if this file resource exists.
     *
     * @return true if exists
     */
    boolean exists();

    /**
     * Creates the directory named by this file resource, including any
     * necessary but nonexistent parent directories.  Note that if this
     * operation fails it may have succeeded in creating some of the necessary
     * parent directories.
     *
     * @return true if all directories have been successfully created
     */
    boolean mkdirs();

    /**
     * Deletes the file resource. If this denotes a directory, then the
     * directory must be empty in order to be deleted.
     *
     * @return true if the file is deleted
     */
    boolean delete();

    /**
     * Returns the file resource of this parent, or
     * <code>null</code> if this file resource does not name a parent directory.
     *
     * @return parent
     */
    Resource getParent();

    /**
     * Returns a list of file resources denoting the files in the
     * directory denoted by this file resource.
     *
     * @return files and sub folders
     */
    List<Resource> list();

    /**
     * Returns a list of path names denoting the files in the
     * directory denoted by this file resource filtered by argument.
     *
     * @param filter filter
     * @return files and sub folders filtered
     */
    List<Resource> list(ResourceFilter filter);

    /**
     * List the available filesystem roots.
     * Can return an empty list, but never null.
     *
     * @return filesystem roots
     */
    List<Resource> listRoots();

    /**
     * Resolve a file that might contain (~,*,?) based on this instance and
     * a given current working directory as argument.
     * The argument can be null.
     *
     * @param cwd current working directory
     * @return resolved files
     */
    List<Resource> resolve(Resource cwd);

    /**
     * OutputStream that will be written to this FileResource
     *
     * @return stream
     * @throws FileNotFoundException if file cannot be written to or is !isLeaf()
     * @param append
     */
    OutputStream write(boolean append) throws FileNotFoundException;

    /**
     * InputStream from this FileResource
     *
     * @return stream
     * @throws FileNotFoundException if the file doesn't exist
     */
    InputStream read() throws FileNotFoundException;


    <A extends BasicFileAttributes> A readAttributes(Class<A> type, LinkOption... options) throws IOException;

    /**
     * Return a new instance of FileResource with String as argument
     *
     * @param path argument
     * @return new instance
     */
    Resource newInstance(String path);

    /**
     * Copy a file/directory to destination;
     *
     * @param destination local that you want copy file
     */
    Resource copy(Resource destination) throws IOException;

    /**
     * Sets the last-modified time of the file or directory named by this abstract pathname.
     *
     * @param time The new last-modified time, measured in milliseconds
     * @return true if and only if the operation succeeded; false otherwise
     */
    boolean setLastModified(long time);

    /**
     * Returns the last modified time of the file or directory
     *
     * @return the new last modified time, in milliseconds
     */
    long lastModified();
}
