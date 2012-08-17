/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.jreadline.console;

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

    public final boolean hasRedirect() {
        return consoleOutput.hasRedirectOrPipe();
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
