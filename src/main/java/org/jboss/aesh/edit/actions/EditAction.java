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
public abstract class EditAction {
    private int start;
    private int end;
    private Action action;

    protected EditAction(int start, Action action) {
        setStart(start);
        setAction(action);
    }

    /**
     * Perform an action
     *
     * @param buffer console
     */
    public abstract void doAction(String buffer);

    private void setAction(Action action) {
        this.action = action;
    }


    public final Action getAction() {
        return action;
    }

    private void setStart(int start) {
        this.start = start;
    }

    public final int getStart() {
        return start;
    }

    protected void setEnd(int end) {
        this.end = end;
    }

    public final int getEnd() {
        return end;
    }

    /**
     * Checks to see if the specified character is a delimiter. We consider a
     * character a delimiter if it is anything but a letter or digit.
     *
     * @param c the character to test
     * @return true if it is a delimiter
     */
    protected final boolean isDelimiter(char c) {
        return !Character.isLetterOrDigit(c);
    }

    /**
     * Checks to see if the specified character is a space.
     *
     * @param c the character to test
     * @return true if the char is a space
     */
    protected final boolean isSpace(char c) {
        return Character.isWhitespace(c);
    }
}
