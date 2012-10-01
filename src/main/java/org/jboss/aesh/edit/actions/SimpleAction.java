/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit.actions;

/**
 * A placeholder class for simple actions that require little logic.
 * Typical movement one char back/forth, to the end/beginning of buffer.
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class SimpleAction extends EditAction {

       public SimpleAction(int start, Action action) {
        super(start, action);
    }

    public SimpleAction(int start, Action action, int end) {
        super(start, action);
        setEnd(end);
    }

    @Override
    public void doAction(String buffer) {
        if(buffer.length() < getEnd())
            setEnd(buffer.length());

        if(getEnd() < 0)
            setEnd(0);
    }

}
