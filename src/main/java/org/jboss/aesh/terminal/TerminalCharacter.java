/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import org.jboss.aesh.util.ANSI;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TerminalCharacter {

    private char character;
    private Color backgroundColor;
    private Color foregroundColor;
    private boolean bold;

    public TerminalCharacter(char c) {
        this(c, Color.DEFAULT, Color.DEFAULT, false);
    }

    public TerminalCharacter(char c, Color background, Color foreground, boolean b) {
        this.character = c;
        this.backgroundColor = background;
        this.foregroundColor = foreground;
        this.bold = b;
    }

    public char getCharacter() {
        return character;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public boolean isBold() {
        return bold;
    }

    public Color getForegroundColor() {
        return foregroundColor;
    }

    public String getTextType() {
        if(bold)
            return ANSI.getBold();
        else
            return ANSI.reset();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TerminalCharacter)) return false;

        TerminalCharacter that = (TerminalCharacter) o;

        if (bold != that.bold) return false;
        if (character != that.character) return false;
        if (backgroundColor != that.backgroundColor) return false;
        if (foregroundColor != that.foregroundColor) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) character;
        result = 31 * result + backgroundColor.hashCode();
        result = 31 * result + foregroundColor.hashCode();
        result = 31 * result + (bold ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TerminalCharacter{" +
                "character=" + character +
                ", backgroundColor=" + backgroundColor +
                ", foregroundColor=" + foregroundColor +
                ", bold=" + bold +
                '}';
    }

 }
