/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit.actions;

/**
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class DeleteAction extends EditAction {

    private boolean backspace = false;

    public DeleteAction(int start, Action action) {
        super(start, action);
    }

    public DeleteAction(int start, Action action, boolean backspace) {
        super(start, action);
        this.backspace = backspace;
    }

    @Override
    public void doAction(String buffer) {
        if(backspace) {
            if(getStart() == 0)
                setEnd(0);
            else
                setEnd(getStart()-1);
        }
        else {
            if(buffer.length() <= getStart())
                setEnd(getStart());
            else
                setEnd(getStart()+1);
        }
    }
}
