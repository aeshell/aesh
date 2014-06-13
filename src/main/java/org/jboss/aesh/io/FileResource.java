/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.io;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * FileResource is a Aesh native io class.
 * The default implementation is based on java.io.File, but it can be specified
 * by the Settings object when it needs to be overridden.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface FileResource {

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
     * Check if this file resource exists.
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
    FileResource getParent();

    /**
     * Returns a list of file resources denoting the files in the
     * directory denoted by this file resource.
     *
     * @return files and sub folders
     */
    List<FileResource> list();

    /**
     * Returns a list of path names denoting the files in the
     * directory denoted by this file resource filtered by argument.
     *
     * @param filter filter
     * @return files and sub folders filtered
     */
    List<FileResource> list(FileResourceFilter filter);

    /**
     * List the available filesystem roots.
     * Can return an empty list, but never null.
     *
     * @return filesystem roots
     */
    List<FileResource> listRoots();

    /**
     * Resolve a file that might contain (~,*,?) based on this instance and
     * a given current working directory as argument.
     * The argument can be null.
     *
     * @param cwd current working directory
     * @return resolved files
     */
    List<FileResource> resolve(FileResource cwd);

    /**
     * OutputStream that will be written to this FileResource
     *
     * @return stream
     * @throws FileNotFoundException if file cannot be written to or is !isLeaf()
     */
    OutputStream writeFileResource() throws FileNotFoundException;

    /**
     * InputStream from this FileResource
     *
     * @return stream
     * @throws FileNotFoundException if the file doesn't exist
     */
    InputStream readFileResource() throws FileNotFoundException;

    /**
     * Return a new instance of FileResource with String as argument
     *
     * @param path argument
     * @return new instance
     */
    FileResource newInstance(String path);

}
