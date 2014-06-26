/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.operator.ControlOperator;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConsoleOperation {

    private ControlOperator controlOperator;
    private String buffer;
    private int pid = -1;


    public ConsoleOperation(ControlOperator controlOperator, String buffer) {
        this.controlOperator = controlOperator;
        this.buffer = buffer;
    }

    public String getBuffer() {
        return buffer;
    }

    public ControlOperator getControlOperator() {
        return controlOperator;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof ConsoleOperation) {
            ConsoleOperation r = (ConsoleOperation) o;
            if(r.getBuffer().equals(getBuffer()) &&
                    r.getControlOperator().equals(getControlOperator()))
                return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 129384;
    }

    @Override
    public String toString() {
        return "ControlOperator: "+ getControlOperator()+", Buffer: "+buffer;
    }

}
