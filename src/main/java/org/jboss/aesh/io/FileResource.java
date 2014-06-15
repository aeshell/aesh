/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.io;

import org.jboss.aesh.io.filter.ResourceFilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Default impl of FileResource, using java.io.File
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class FileResource implements Resource {

    private File file;

    public FileResource(File file) {
        if(file == null)
            throw new IllegalArgumentException("file argument cant be null");
        this.file = file;
    }

    public FileResource(String file) {
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
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public boolean isSymbolicLink() {
        return Files.isSymbolicLink(file.toPath());
    }

    @Override
    public Resource readSymbolicLink() throws IOException {
        if(isSymbolicLink())
            return new FileResource( Files.readSymbolicLink(file.toPath()).toFile());
        else
            return new FileResource("");
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
    public boolean delete() {
        return file.delete();
    }

    @Override
    public Resource getParent() {
        return new FileResource(file.getParentFile());
    }

    @Override
    public List<Resource> list() {
        List<Resource> files = new ArrayList<>();

        if(file != null) {
            File[] listFiles = file.listFiles();
            if(listFiles != null)
                for (File f : listFiles)
                    files.add(new FileResource(f));
        }

        return files;
    }

    @Override
    public List<Resource> list(ResourceFilter filter) {
        List<Resource> files = new ArrayList<>();
        for(Resource f : list()) {
            if(filter != null && filter.accept(f))
                files.add(f);
        }

        return files;
    }

    @Override
    public List<Resource> listRoots() {
        List<Resource> files = new ArrayList<>();
        for(File f : File.listRoots())
           files.add(new FileResource(f));

        return files;
    }

    @Override
    public List<Resource> resolve(Resource cwd) {
        List<Resource> files = new ArrayList<>();
        for(File f : PathResolver.resolvePath(getFile(), ((FileResource) cwd).getFile()))
            files.add(new FileResource(f));

        return files;
    }

    @Override
    public InputStream read() throws FileNotFoundException {
        return new FileInputStream(file);
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Class<A> type, LinkOption... options) throws IOException {
        return Files.readAttributes(file.toPath(), type, options);
    }

    @Override
    public OutputStream write() throws FileNotFoundException {
        return new FileOutputStream(file);
    }

    @Override
    public String toString() {
        return file.toString();
    }

    @Override
    public Resource newInstance(String path) {
        return new FileResource(path);
    }

    @Override
    public Resource copy(Resource destination) throws IOException {
        return new FileResource(Files.copy(file.toPath(), new FileResource(destination.getAbsolutePath()).getFile().toPath()).toFile());
    }

    public File getFile() {
        return file;
    }

}
