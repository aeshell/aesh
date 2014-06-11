/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.io;

import org.jboss.aesh.util.PathResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Default impl of FileResource, using java.io.File
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class DefaultFileResource implements FileResource {

    private File file;

    public DefaultFileResource(File file) {
        if(file == null)
            throw new IllegalArgumentException("file argument cant be null");
        this.file = file;
    }

    public DefaultFileResource(String file) {
        if(file == null)
            throw new IllegalArgumentException("file argument cant be null");
        this.file = new File(file);
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    @Override
    public boolean isLeaf() {
        return file.isFile();
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public boolean mkdirs() {
        return file.mkdirs();
    }

    @Override
    public FileResource getParentResource() {
        return new DefaultFileResource(file.getParentFile());
    }

    @Override
    public List<FileResource> listFileResources() {
        List<FileResource> files = new ArrayList<>();

        if(file != null) {
            File[] listFiles = file.listFiles();
            if(listFiles != null)
                for (File f : listFiles)
                    files.add(new DefaultFileResource(f));
        }

        return files;
    }

    @Override
    public List<FileResource> listFileResources(FileResourceFilter filter) {
        List<FileResource> files = new ArrayList<>();
        for(FileResource f : listFileResources()) {
            if(filter != null && filter.accept(f))
                files.add(f);
        }

        return files;
    }

    @Override
    public List<FileResource> listRoots() {
        List<FileResource> files = new ArrayList<>();
        for(File f : File.listRoots())
           files.add(new DefaultFileResource(f));

        return files;
    }

    @Override
    public List<FileResource> resolve(FileResource cwd) {
        List<FileResource> files = new ArrayList<>();
        for(File f : PathResolver.resolvePath( getFile(), ((DefaultFileResource) cwd).getFile()))
            files.add(new DefaultFileResource(f));

        return files;
    }

    @Override
    public InputStream readFileResource() throws FileNotFoundException {
        return new FileInputStream(file);
    }

    @Override
    public OutputStream writeFileResource() throws FileNotFoundException {
        return new FileOutputStream(file);
    }

    @Override
    public String toString() {
        return file.toString();
    }

    @Override
    public FileResource newInstance(String path) {
        return new DefaultFileResource(path);
    }

    public File getFile() {
        return file;
    }

}
