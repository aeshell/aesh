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

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class PipelineResource implements Resource {

    private final BufferedInputStream inputStream;

    public PipelineResource(BufferedInputStream inputStream) {
       this.inputStream = inputStream;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getAbsolutePath() {
        return "";
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    @Override
    public Resource readSymbolicLink() throws IOException {
        return null;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public boolean mkdirs() {
        return false;
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public void move(Resource target) throws IOException {
        //NO OP
    }

    @Override
    public Resource getParent() {
        return null;
    }

    @Override
    public List<Resource> list() {
        return new ArrayList<>();
    }

    @Override
    public List<Resource> list(ResourceFilter filter) {
        return new ArrayList<>();
    }

    @Override
    public List<Resource> listRoots() {
        return new ArrayList<>();
    }

    @Override
    public List<Resource> resolve(Resource cwd) {
        return new ArrayList<>();
    }

    @Override
    public OutputStream write(boolean append) throws FileNotFoundException {
        return null;
    }

    @Override
    public InputStream read() throws FileNotFoundException {
        return inputStream;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Class<A> type, LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public Resource newInstance(String path) {
        return new FileResource(path);
    }

    @Override
    public Resource copy(Resource destination) throws IOException {

        if(destination instanceof FileResource) {
            java.nio.file.Files.copy(
                    inputStream,
                    ((FileResource) destination).getFile().toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }

        return destination;
    }

    @Override
    public boolean setLastModified(long time) {
        return false;
    }

    @Override
    public long lastModified() {
        return 0;
    }

    @Override
    public void setLastAccessed(long time) throws IOException {

    }

    @Override
    public long lastAccessed() throws IOException {
        return 0;
    }
}
