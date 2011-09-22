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
package org.jboss.jreadline.console;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class Buffer {

    private int cursor = 0;
    private StringBuilder line;
    private String prompt;

    private final static int TAB = 4;

    protected Buffer() {
        this(null);
    }

    protected Buffer(String promptText) {
        if(promptText != null)
            prompt = promptText;
        line = new StringBuilder();
    }

    protected void reset(String promptText) {
        if(promptText != null)
            prompt = promptText;
        else
          prompt = "";
        cursor = 0;
        line = new StringBuilder();
    }

    protected int length() {
        return line.length();
    }

    protected int totalLength() {
        return line.length() + prompt.length();
    }

    protected int getCursor() {
        return cursor;
    }

    protected int getCursorWithPrompt() {
        return cursor + prompt.length();
    }

    protected String getPrompt() {
        return prompt;
    }

    protected void setCursor(int cursor) {
        this.cursor = cursor ;
    }

    protected char[] syncMove(int move) {
        return move(move, true);
    }

    protected char[] move(int move) {
        return move(move, false);
    }

    private char[] move(int move, boolean keepCursorPosition) {
        move = moveCursor(move);

        if(!keepCursorPosition)
            setCursor(getCursor() + move);

        if(move < 0)
            return printAnsi(Math.abs(move)+"D");

        else if(move > 0)
            return printAnsi(move+"C");
        else
            return new char[0];
    }

    /**
     * Return a ansified string based on param
     *
     * @param out string
     * @return ansified string
     */
    public static char[] printAnsi(String out) {
        return printAnsi(out.toCharArray());
    }

    public static char[] printAnsi(char[] out) {
        //calculate length of table:
        int length = 0;
        for(char c : out) {
          if(c == '\t') {
              length += TAB;
          }
          else
            length++;
        }

        char[] ansi = new char[length+2];
        ansi[0] = (char) 27;
        ansi[1] = '[';
        int counter = 0;
        for(int i=0; i < out.length; i++) {
            if(out[i] == '\t') {
                Arrays.fill(ansi, counter+2, counter+2+TAB, ' ');
                counter += TAB-1;
            }
            else
                ansi[counter+2] = out[i];

            counter++;
        }

        return ansi;
    }

    /**
     * Make sure that the cursor do not move ob (out of bounds)
     *
     * @param move, negative values for moving left,positive for right
     * @return adjusted movement
     */
    protected final int moveCursor(final int move) {
        // cant move to a negative value
        if(getCursor() == 0 && move <=0 )
            return 0;
        // cant move longer than the length of the line
        if(getCursor() == line.length() && (move > 0))
            return 0;

        // dont move out of bounds
        if(getCursor() + move > line.length())
            return (line.length()-getCursor());

        if(getCursor() + move < 0)
            return -getCursor();

        return move;

    }

    protected char[] getLineFrom(int position) {
        return line.substring(position).toCharArray();
    }

    public StringBuilder getLine() {
        return line;
    }

    protected void setLine(StringBuilder line) {
        this.line = line;
    }

    public void write(char c) {
        line.insert(cursor++, c);
    }

    public void write(final String str) {
        assert str != null;

        if (line.length() == 0) {
            line.append(str);
        }
        else {
            line.insert(cursor, str);
        }

        cursor += str.length();
    }

    protected void clear() {
        line = new StringBuilder();
    }

    /**
     * Switch case if the current character is a letter.
     *
     * @return false if the character is not a letter, else true
     */
    protected boolean changeCase() {
        char c = getLine().charAt(getCursor());
        if(Character.isLetter(c)) {
            if(Character.isLowerCase(c))
                getLine().setCharAt(getCursor(), Character.toUpperCase(c));
            else
                getLine().setCharAt(getCursor(), Character.toLowerCase(c));

            return true;
        }
        else
            return false;
    }

    protected String findStartsWith(List<String> completionList) {
        StringBuilder builder = new StringBuilder();
        for(String completion : completionList)
            while(builder.length() < completion.length() &&
                  startsWith(completion.substring(0, builder.length()+1), completionList))
                builder.append(completion.charAt(builder.length()));

        return builder.toString();
    }

    private boolean startsWith(String criteria, List<String> completionList) {
        for(String completion : completionList)
            if(!completion.startsWith(criteria))
                return false;

        return true;
    }
}
