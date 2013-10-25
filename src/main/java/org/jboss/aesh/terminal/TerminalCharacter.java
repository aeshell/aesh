/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import org.jboss.aesh.util.ANSI;

/**
 * Value object that describe how a terminal character should be displayed
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TerminalCharacter {

    private char character;
    private Color backgroundColor;
    private Color textColor;
    private TerminalTextStyle style;

    public TerminalCharacter(char c) {
        this(c, new TerminalTextStyle());
    }

    public TerminalCharacter(char c, TerminalTextStyle style) {
        this.character = c;
        this.style = style;
        textColor = Color.DEFAULT_TEXT;
        backgroundColor = Color.DEFAULT_BG;
    }

    public TerminalCharacter(char c, Color background, Color text) {
        this(c,background, text, new TerminalTextStyle());
    }

    public TerminalCharacter(char c, Color background, Color text,
                             CharacterType type) {
        this(c, background, text, new TerminalTextStyle(type));
    }

    public TerminalCharacter(char c, Color background, Color text,
                             TerminalTextStyle style) {
        this(c, style);
        this.backgroundColor = background;
        this.textColor = text;
    }

    public char getCharacter() {
        return character;
    }

    public TerminalTextStyle getStyle() {
        return style;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getTextColor() {
        return textColor;
    }

    /**
     * style, text color, background color
     */
    public String toString(TerminalCharacter prev) {
        if(equalsIgnoreCharacter(prev))
            return String.valueOf(character);
        else {
            StringBuilder builder = new StringBuilder();
            builder.append(ANSI.getStart());
            builder.append(style.getValueComparedToPrev(prev.getStyle()));
            if(this.getTextColor() != prev.getTextColor() ||
                    prev.getStyle().isInvert())
                builder.append(';').append(this.getTextColor().getValue());
            if(this.getBackgroundColor() != prev.getBackgroundColor() ||
                    prev.getStyle().isInvert())
                builder.append(';').append(this.getBackgroundColor().getValue());

            builder.append('m');
            builder.append(getCharacter());
            return builder.toString();
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(ANSI.getStart());
        builder.append(style.toString()).append(';');
        builder.append(this.getTextColor().getValue()).append(';');
        builder.append(this.getBackgroundColor().getValue());
        builder.append('m');
        builder.append(getCharacter());
        return builder.toString();
    }

    public boolean equalsIgnoreCharacter(TerminalCharacter that) {
        if (style != that.style) return false;
        if (backgroundColor != that.backgroundColor) return false;
        if (textColor != that.textColor) return false;

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TerminalCharacter)) return false;

        TerminalCharacter that = (TerminalCharacter) o;

        if (style != that.style) return false;
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
        result = 31 * result + style.hashCode();
        return result;
    }

 }
