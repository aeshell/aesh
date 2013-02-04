/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import org.jboss.aesh.util.ANSI;

/**
 * Describe how a terminal character should be displayed
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TerminalCharacter {

    private char character;
    private Color backgroundColor;
    private Color textColor;
    private CharacterType type;

    public TerminalCharacter(char c) {
        this(c, CharacterType.NORMAL);
    }

    public TerminalCharacter(char c, CharacterType type) {
        this.character = c;
        this.type = type;
        textColor = Color.DEFAULT_TEXT;
        backgroundColor = Color.DEFAULT_BG;
    }

    public TerminalCharacter(char c, Color background, Color text) {
        this(c,background, text, CharacterType.NORMAL);
    }

    public TerminalCharacter(char c, Color background, Color text,
                             CharacterType type) {
        this(c, type);
        this.backgroundColor = background;
        this.textColor = text;
    }

    public char getCharacter() {
        return character;
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
    public String getAsString(TerminalCharacter prev) {
        if(equalsIgnoreCharacter(prev))
            return String.valueOf(character);
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
            builder.append(getCharacter());
            return builder.toString();
        }
    }

    public String getAsString() {
        StringBuilder builder = new StringBuilder();
        builder.append(ANSI.getStart());
        builder.append(type.getValue()).append(';');
        builder.append(this.getTextColor().getValue()).append(';');
        builder.append(this.getBackgroundColor().getValue());
        builder.append('m');
        builder.append(getCharacter());
        return builder.toString();
    }

    public boolean equalsIgnoreCharacter(TerminalCharacter that) {
        if (type != that.type) return false;
        if (backgroundColor != that.backgroundColor) return false;
        if (textColor != that.textColor) return false;

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TerminalCharacter)) return false;

        TerminalCharacter that = (TerminalCharacter) o;

        if (type != that.type) return false;
        if (character != that.character) return false;
        if (backgroundColor != that.backgroundColor) return false;
        if (textColor != that.textColor) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) character;
        result = 31 * result + backgroundColor.hashCode();
        result = 31 * result + textColor.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TerminalCharacter{" +
                "character=" + character +
                ", backgroundColor=" + backgroundColor +
                ", textColor=" + textColor +
                ", type=" + type +
                '}';
    }

 }
