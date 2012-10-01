/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.undo;

import java.util.Stack;

/**
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class UndoManager {

    private static short UNDO_SIZE = 50;

    private Stack<UndoAction> undoStack;
    private int counter;

    public UndoManager() {
        undoStack = new Stack<UndoAction>();
        undoStack.setSize(UNDO_SIZE);
        counter = 0;
    }

    public UndoAction getNext() {
        if(counter > 0) {
            counter--;
            return undoStack.pop();
        }
        else
            return null;
    }

    public void addUndo(UndoAction u) {
        if(counter <= UNDO_SIZE) {
            counter++;
            undoStack.push(u);
        }
        else {
            undoStack.remove(UNDO_SIZE);
            undoStack.push(u);
        }

    }

    public void clear() {
        undoStack.clear();
        counter = 0;
    }

    public boolean isEmpty() {
        return (counter == 0);
    }

    public int size() {
        return counter;
    }
}
