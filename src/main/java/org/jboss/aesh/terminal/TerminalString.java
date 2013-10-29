/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.util.ANSI;

import java.io.PrintStream;

/**
 * Value object that describe how a string should be displayed
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TerminalString {

    private String characters;
    private TerminalTextStyle style;
    private TerminalColor color;
    private boolean ignoreRendering;
    private int ansiLength = 0;

    public TerminalString(String chars, TerminalColor color, TerminalTextStyle style) {
        this.characters = chars;
        this.color = color;
        this.style = style;
    }

    public TerminalString(String chars, TerminalColor color) {
        this(chars, color, new TerminalTextStyle());
    }

    public TerminalString(String chars, TerminalTextStyle style) {
        this(chars, new TerminalColor(), style);
    }

    public TerminalString(String chars) {
        this(chars, new TerminalColor(), new TerminalTextStyle());
    }

    public TerminalString(String chars, boolean ignoreRendering) {
        this(chars, new TerminalColor(), new TerminalTextStyle());
        this.ignoreRendering = ignoreRendering;
    }

    public String getCharacters() {
        return characters;
    }

    public void setCharacters(String chars) {
        this.characters = chars;
    }

    public boolean containSpaces() {
        return characters.indexOf(Parser.SPACE_CHAR) > 0;
    }

    public void switchSpacesToEscapedSpaces() {
       characters = Parser.switchSpacesToEscapedSpacesInWord(characters);
    }

    public TerminalTextStyle getStyle() {
        return style;
    }

    public int getANSILength() {
        if(ignoreRendering)
            return 0;
        else {
            if (ansiLength == 0)
                ansiLength = ANSI.getStart().length() + color.getLength() +
                        style.getLength() + ANSI.reset().length() +2 ; // ; + m
            return ansiLength;
        }
    }

    public TerminalString cloneRenderingAttributes(String chars) {
        if(ignoreRendering)
            return new TerminalString(chars, true);
        else
            return new TerminalString(chars, color, style);
    }

    /**
     * style, text color, background color
     */
    public String toString(TerminalString prev) {
        if(ignoreRendering)
            return characters;
        if(equalsIgnoreCharacter(prev))
            return characters;
        else {
            StringBuilder builder = new StringBuilder();
            builder.append(ANSI.getStart())
                    .append(style.getValueComparedToPrev(prev.getStyle()));

            if(!this.color.equals(prev.color)) {
                if(prev.getStyle().isInvert())
                    builder.append(';').append(this.color.toString());
                else
                    builder.append(';').append(this.color.toString(prev.color));
            }

            builder.append('m').append(getCharacters());
            return builder.toString();
        }
    }

    @Override
    public String toString() {
        if(ignoreRendering)
            return characters;
        StringBuilder builder = new StringBuilder();
        builder.append(ANSI.getStart())
                .append(style.toString())
                .append(';')
                .append(this.color.toString())
                .append('m')
                .append(getCharacters())
                        //reset it to plain
                .append(ANSI.reset());
        return builder.toString();
    }

    public void write(PrintStream out) {
        if(ignoreRendering) {
            out.print(characters);
        }
        else {
            out.print(ANSI.getStart());
            out.print(style.toString());
            out.print(';');
            this.color.write(out);
            out.print('m');
            out.print(getCharacters());
        }
    }

    public boolean equalsIgnoreCharacter(TerminalString that) {
        if (style != that.style) return false;
        if (ignoreRendering != that.ignoreRendering) return false;
        if (!color.equals(that.color)) return false;

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TerminalString)) return false;

        TerminalString that = (TerminalString) o;

        if(ignoreRendering) {
            return characters.equals(that.characters);
        }

        if (!characters.equals(that.characters)) return false;
        if (!color.equals(that.color)) return false;
        if (style != that.style) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = characters.hashCode();
        result = 31 * result + color.hashCode();
        result = 31 * result + style.hashCode();
        return result;
    }

}
