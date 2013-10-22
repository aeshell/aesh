/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import org.jboss.aesh.util.ANSI;

import java.io.PrintStream;

/**
 * Value object that describe how a string should be displayed
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TerminalString {

    private String characters;
    private Color backgroundColor;
    private Color textColor;
    private CharacterType type;

    public TerminalString(String chars, Color backgroundColor, Color textColor,
                          CharacterType type) {
        this.characters = chars;
        this.type = type;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
    }

    public TerminalString(String chars, Color backgroundColor, Color textColor) {
        this(chars, backgroundColor, textColor, CharacterType.NORMAL);
    }

    public TerminalString(String chars, CharacterType type) {
        this(chars, Color.DEFAULT_BG, Color.DEFAULT_TEXT, type);
    }

    public TerminalString(String chars) {
        this(chars, Color.DEFAULT_BG, Color.DEFAULT_TEXT, CharacterType.NORMAL);
    }

    public String getCharacters() {
        return characters;
    }

    public void setCharacters(String characters) {
        this.characters = characters;
    }

    public CharacterType getType() {
        return type;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getTextColor() {
        return textColor;
    }

    /**
     * type, text color, background color
     */
    public String toString(TerminalString prev) {
        if(equalsIgnoreCharacter(prev))
            return characters;
        else {
            StringBuilder builder = new StringBuilder();
            builder.append(ANSI.getStart());
            builder.append(type.getValueComparedToPrev(prev.getType()));
            if(this.getTextColor() != prev.getTextColor() ||
                    prev.getType() == CharacterType.INVERT)
                builder.append(';').append(this.getTextColor().getValue());
            if(this.getBackgroundColor() != prev.getBackgroundColor() ||
                    prev.getType() == CharacterType.INVERT)
                builder.append(';').append(this.getBackgroundColor().getValue());

            builder.append('m');
            builder.append(getCharacters());
            return builder.toString();
        }
    }

    @Override
    public String toString() {
        //Thread.dumpStack();
        //return getCharacters();
        StringBuilder builder = new StringBuilder();
        builder.append(ANSI.getStart());
        builder.append(type.getValue()).append(';');
        builder.append(this.getTextColor().getValue()).append(';');
        builder.append(this.getBackgroundColor().getValue());
        builder.append('m');
        builder.append(getCharacters());
        return builder.toString();
    }

    public void write(PrintStream out) {
        out.print(ANSI.getStart());
        out.print(type.getValue());
        out.print(';');
        out.print(this.getTextColor().getValue());
        out.print(';');
        out.print(this.getBackgroundColor().getValue());
        out.print('m');
        out.print(getCharacters());
        out.flush();
    }

    public boolean equalsIgnoreCharacter(TerminalString that) {
        if (type != that.type) return false;
        if (backgroundColor != that.backgroundColor) return false;
        if (textColor != that.textColor) return false;

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TerminalString)) return false;

        TerminalString that = (TerminalString) o;

        if (backgroundColor != that.backgroundColor) return false;
        if (!characters.equals(that.characters)) return false;
        if (textColor != that.textColor) return false;
        if (type != that.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = characters.hashCode();
        result = 31 * result + backgroundColor.hashCode();
        result = 31 * result + textColor.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

}
