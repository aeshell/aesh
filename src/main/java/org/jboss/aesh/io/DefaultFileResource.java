/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.io;

import java.io.File;
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

    @Override
    public String getName() {
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
    public boolean mkdir() {
        return file.mkdir();
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
    public List<FileResource> resolve(FileResource incPath, FileResource cwd) {
        return null;
    }

}
