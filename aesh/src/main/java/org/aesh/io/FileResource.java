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
package org.aesh.io;

import org.aesh.io.filter.ResourceFilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Default impl of Resource, using java.io.File
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

    public FileResource(Path file) {
        if(file == null)
            throw new IllegalArgumentException("file argument cant be null");
        this.file = file.toFile();
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
    public void move(Resource target) throws IOException {
        FileResource destination = (FileResource) target;
        Files.move(this.file.toPath(), destination.file.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
        if(file.getPath().startsWith("~"+File.separatorChar))
            file = new File(System.getProperty("user.home") + file.getPath().substring(1));
        return new FileInputStream(file);
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Class<A> type, LinkOption... options) throws IOException {
        return Files.readAttributes(file.toPath(), type, options);
    }

    @Override
    public OutputStream write(boolean append) throws FileNotFoundException {
        return new FileOutputStream(file, append);
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

    @Override
    public boolean setLastModified(long time) {
        return file.setLastModified(time);
    }

    @Override
    public long lastModified() {
        return file.lastModified();
    }

    @Override
    public void setLastAccessed(long time) throws IOException {
        FileTime fileTime = FileTime.fromMillis(time);
        Files.setAttribute(file.toPath(), "lastAccessTime", fileTime);
    }

    @Override
    public long lastAccessed() throws IOException {
        BasicFileAttributes bfa = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        FileTime lastAccessed = bfa.lastAccessTime();
        return lastAccessed.toMillis();
    }

    public File getFile() {
        return file;
    }
}
