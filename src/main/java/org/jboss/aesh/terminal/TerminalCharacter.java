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
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
            builder.append(ANSI.START);
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
            cache = ANSI.START + style.toString() + ';' + this.color.toString() + 'm' + getCharacter();
        }
        return cache;
    }

    public boolean equalsIgnoreCharacter(TerminalCharacter that) {
        return style.equals(that.style) && color.equals(that.color);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TerminalCharacter)) return false;

        TerminalCharacter that = (TerminalCharacter) o;

        return style.equals(that.style) &&
                character == that.character &&
                color.equals(that.color);
    }

    @Override
    public int hashCode() {
        int result = (int) character;
        result = 31 * result + color.hashCode();
        result = 31 * result + style.hashCode();
        return result;
    }

 }
