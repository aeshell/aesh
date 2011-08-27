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
package org.jboss.jreadline.edit.actions;

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
    public abstract void doAction(StringBuilder buffer);

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
