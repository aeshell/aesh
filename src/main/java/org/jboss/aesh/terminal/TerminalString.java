/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
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
 */ackage org.jboss.aesh.terminal;

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
        return ANSI.getStart() + style.toString() + ';' +
                this.color.toString() +
                'm' + getCharacters() + ANSI.reset();
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
