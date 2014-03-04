/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.edit.EditMode;
import org.jboss.aesh.edit.EmacsEditMode;
import org.jboss.aesh.edit.KeyOperationFactory;
import org.jboss.aesh.edit.KeyOperationManager;
import org.jboss.aesh.terminal.Shell;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshConsoleBufferBuilder {

    private Prompt prompt;
    private Shell shell;
    private EditMode editMode;

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

    public ConsoleBuffer create() {
        if(shell == null)
            throw new IllegalArgumentException("Shell must be provided to create ConsoleBuffer");
        if(editMode == null)
            editMode = new EmacsEditMode(new KeyOperationManager(KeyOperationFactory.generateEmacsMode()));
        if(prompt == null)
            prompt = new Prompt("");

        return new AeshConsoleBuffer(prompt, shell, editMode);
    }
}
