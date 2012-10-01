/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.undo;

/**
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class UndoAction {

    private int cursorPosition;
    private String buffer;

    public UndoAction(int cursorPosition, String buffer) {
        setCursorPosition(cursorPosition);
        setBuffer(buffer);
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    private void setCursorPosition(int cursorPosition) {
        this.cursorPosition = cursorPosition;
    }

    public String getBuffer() {
        return buffer;
    }

    private void setBuffer(String buffer) {
        this.buffer = buffer;
    }
}
