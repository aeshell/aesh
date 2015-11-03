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
package org.jboss.aesh.console;


import org.jboss.aesh.readline.editing.EditMode;
import org.jboss.aesh.readline.editing.EditModeBuilder;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshConsoleBufferBuilder {

    private Prompt prompt;
    private Shell shell;
    private EditMode editMode;
    private boolean ansiMode = true;

    public AeshConsoleBufferBuilder() {
    }

    public AeshConsoleBufferBuilder prompt(Prompt prompt) {
        this.prompt = prompt;
        return this;
    }

    public AeshConsoleBufferBuilder shell(Shell shell) {
        this.shell = shell;
        return this;
    }

    public AeshConsoleBufferBuilder editMode(EditMode editMode) {
        this.editMode = editMode;
        return this;
    }

    public AeshConsoleBufferBuilder ansi(boolean ansi) {
        this.ansiMode = ansi;
        return this;
    }

    public ConsoleBuffer create() {
        if(shell == null)
            throw new IllegalArgumentException("Shell must be provided to create ConsoleBuffer");
        if(editMode == null)
            editMode = new EditModeBuilder().create();
        if(prompt == null)
            prompt = new Prompt("");

        return new AeshConsoleBuffer(prompt, shell, editMode, ansiMode);
    }
}
