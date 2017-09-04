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
package org.aesh.readline;

import org.aesh.io.FileResource;
import org.aesh.io.Resource;
import org.aesh.utils.Config;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class DefaultAeshContext implements AeshContext {

    private Resource cwd;

    public DefaultAeshContext() {
        this(new FileResource("").newInstance(Config.getUserDir()));
    }

    public DefaultAeshContext(Resource cwd) {
        if(cwd != null && (!cwd.isLeaf() && cwd.exists()))
            this.cwd = cwd;
        else
            throw new IllegalArgumentException("Current working directory must be a directory");
    }

    @Override
    public Resource getCurrentWorkingDirectory() {
        return cwd;
    }

    @Override
    public void setCurrentWorkingDirectory(Resource cwd) {
        if(!cwd.isLeaf())
            this.cwd = cwd;
        else
            throw new IllegalArgumentException("Current working directory must be a directory");
    }
}
