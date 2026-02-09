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

import java.util.Set;

import org.aesh.io.Resource;

/**
 * @author Aesh team
 */
public interface AeshContext {

    /**
     * The current working directory.
     * It is up to the running cli to keep the cwd updated to the correct value
     *
     * @return cwd
     */
    Resource getCurrentWorkingDirectory();

    /**
     * Set the current working directory
     *
     * @param cwd cwd
     */
    void setCurrentWorkingDirectory(Resource cwd);

    /**
     * If export is enabled this will return a set of all the
     * variable keys
     *
     * @return set of exported variable keys
     */
    Set<String> exportedVariableNames();

    /**
     * @param key variable key
     * @return value of the variable key if it is defined, else null
     */
    String exportedVariable(String key);
}
