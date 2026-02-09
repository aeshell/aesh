/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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
package org.aesh.console;

import java.util.Collections;
import java.util.Set;

import org.aesh.command.export.ExportManager;
import org.aesh.io.FileResource;
import org.aesh.io.Resource;
import org.aesh.terminal.utils.Config;

/**
 * @author Aesh team
 */
public class DefaultAeshContext implements AeshContext {

    private Resource cwd;
    private final ExportManager exportManager;

    public DefaultAeshContext() {
        this(new FileResource("").newInstance(Config.getUserDir()), null);
    }

    public DefaultAeshContext(Resource cwd) {
        this(cwd, null);
    }

    public DefaultAeshContext(Resource cwd, ExportManager exportManager) {
        if (cwd != null && (!cwd.isLeaf() && cwd.exists()))
            this.cwd = cwd;
        else
            throw new IllegalArgumentException("Current working directory must be a directory");

        this.exportManager = exportManager;
    }

    public DefaultAeshContext(ExportManager exportManager) {
        this(new FileResource("").newInstance(Config.getUserDir()), exportManager);
    }

    @Override
    public Resource getCurrentWorkingDirectory() {
        return cwd;
    }

    @Override
    public void setCurrentWorkingDirectory(Resource cwd) {
        if (!cwd.isLeaf())
            this.cwd = cwd;
        else
            throw new IllegalArgumentException("Current working directory must be a directory");
    }

    @Override
    public Set<String> exportedVariableNames() {
        if (exportManager != null)
            return exportManager.keys();
        else
            return Collections.emptySet();

    }

    @Override
    public String exportedVariable(String key) {
        if (exportManager != null)
            return exportManager.getValue(key);
        else
            return null;
    }
}
