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
    private TerminalTextStyle style;
    private TerminalColor color;
    private String cache;

    public TerminalCharacter(char c) {
        this(c, new TerminalTextStyle());
    }

    public TerminalCharacter(char c, TerminalTextStyle style) {
        this(c, new TerminalColor(), style);
    }

    public TerminalCharacter(char c, TerminalColor color) {
        this(c, color, new TerminalTextStyle());
    }

    public TerminalCharacter(char c, TerminalColor color,
                             CharacterType type) {
        this(c, color, new TerminalTextStyle(type));
    }

    public TerminalCharacter(char c, TerminalColor color,
                             TerminalTextStyle style) {
        this.character = c;
        this.style = style;
        this.color = color;
    }

    public char getCharacter() {
        return character;
    }
    public void setCharacter(char c) {
        this.character = c;
        cache = null;
    }

    public TerminalTextStyle getStyle() {
        return style;
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
            if(!style.equals(prev.getStyle())) {
                builder.append(style.getValueComparedToPrev(prev.getStyle()));
            }
            if(!this.color.equals(prev.color)) {
                if(prev.getStyle().isInvert()) {
                    if(builder.charAt(builder.length()-1) == '[')
                        builder.append(this.color.toString());
                    else
                        builder.append(';').append(this.color.toString());
                }
                else {
                    if(builder.charAt(builder.length()-1) == '[')
                        builder.append(this.color.toString(prev.color));
                    else
                        builder.append(';').append(this.color.toString(prev.color));
                }
            }

            builder.append('m');
            builder.append(getCharacter());
            return builder.toString();
        }
    }

    @Override
    public String toString() {
        if(cache == null) {
            StringBuilder builder = new StringBuilder();
            builder.append(ANSI.getStart());
            builder.append(style.toString()).append(';');
            builder.append(this.color.toString());
            builder.append('m');
            builder.append(getCharacter());
            cache = builder.toString();
        }
        return cache;
    }

    public boolean equalsIgnoreCharacter(TerminalCharacter that) {
        if (!style.equals(that.style)) return false;
        if (!color.equals(that.color)) return false;

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TerminalCharacter)) return false;

        TerminalCharacter that = (TerminalCharacter) o;

        if (!style.equals(that.style)) return false;
        if (character != that.character) return false;
        if (!color.equals(that.color)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) character;
        result = 31 * result + color.hashCode();
        result = 31 * result + style.hashCode();
        return result;
    }

 }
