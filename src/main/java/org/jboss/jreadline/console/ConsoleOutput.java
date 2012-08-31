/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.console;

import org.jboss.jreadline.console.operator.ControlOperator;

/**
 * Value object returned by Console when newline is pressed
 * If the command is part of a pipeline sequence the stdOut and stdErr is populated accordingly
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConsoleOutput {

    private String stdOut;
    private String stdErr;
    private ConsoleOperation consoleOperation;

    public ConsoleOutput(ConsoleOperation consoleOperation) {
       this.consoleOperation = consoleOperation;
    }

   public ConsoleOutput(ConsoleOperation consoleOperation, String stdOut, String stdErr) {
        this(consoleOperation);
        this.stdOut = stdOut;
        this.stdErr = stdErr;
    }

    public String getBuffer() {
        return consoleOperation.getBuffer();
    }

    public ControlOperator getControlOperator() {
        return consoleOperation.getControlOperator();
    }

    public String getStdOut() {
        return stdOut;
    }

    public String getStdErr() {
        return stdErr;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("Buffer: ").append(getBuffer())
                .append("\nControlOperator: ").append(getControlOperator())
                .append("\nStdOut: ").append(getStdOut())
                .append("\nStdErr: ").append(getStdErr()).toString();
    }
}
