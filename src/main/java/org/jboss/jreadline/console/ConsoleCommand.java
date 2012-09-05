/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.console;

import org.jboss.jreadline.console.operator.ControlOperator;
import org.jboss.jreadline.edit.actions.Operation;

import java.io.IOException;

/**
 * A ConsoleCommand is the base of any "external" commands that will run
 * in the foreground of jreadline.
 * Call attach() to set a command in the foreground of jreadline.
 *
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public abstract class ConsoleCommand {

    boolean attached = false;
    protected Console console = null;
    ConsoleOutput consoleOutput;

    public ConsoleCommand(Console console) {
        this.console = console;
    }

    /**
     * Called by creator of the process
     * Calls afterAttach()
     *
     * @throws IOException stream
     */
    public final void attach(ConsoleOutput output) throws IOException {
        attached = true;
        this.console.attachProcess(this);
        this.consoleOutput = output;
        afterAttach();
    }

    /**
     *
     * @return true if the process is attached to console. eg. its "running".
     */
    public final boolean isAttached() {
        return attached;
    }

    /**
     * Mark this process ready to be detached from console.
     * Calls afterDetach
     *
     * @throws IOException stream
     */
    public final void detach() throws IOException {
        attached = false;
        afterDetach();
    }

    public final boolean hasRedirectOut() {
        return ControlOperator.isRedirectionOut(consoleOutput.getControlOperator());
    }

    public final ConsoleOutput getConsoleOutput() {
        return consoleOutput;
    }

    /**
     * Called after attach(..) is called.
     *
     * @throws IOException stream
     */
    protected abstract void afterAttach() throws IOException;

    /**
     * Called after detach() is called
     *
     * @throws IOException stream
     */
    protected abstract void afterDetach() throws IOException;

    /**
     * Called after every operation made by the user
     *
     * @param operation operation
     * @throws IOException stream
     */
    public abstract void processOperation(Operation operation) throws IOException;
}
