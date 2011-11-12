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
 * A simple buffer to keep track of one line in the console
 * and the cursor position.
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class Buffer {

    private int cursor = 0;
    private StringBuilder line;
    private String prompt;
    private int delta;

    private final static int TAB = 4;

    protected Buffer() {
        this(null);
    }

    /**
     * Instantiate a Buffer with given prompt
     *
     * @param promptText set prompt
     */
    protected Buffer(String promptText) {
        if(promptText != null)
            prompt = promptText;
        else
            prompt = "";

        line = new StringBuilder();
        delta = 0;
    }

    /**
     * Reset the buffer
     *
     * @param promptText set prompt
     */
    protected void reset(String promptText) {
        if(promptText != null)
            prompt = promptText;
        else
          prompt = "";
        cursor = 0;
        line = new StringBuilder();
        delta = 0;
    }

    /**
     * @return length of the line without prompt
     */
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

    /**
     * Move the cursor left if the param is negative,
     * and right if its positive.
     * Return ansi code to represent the move
     *
     * @param move where to move
     * @return ansi string that represent the move
     */
    protected char[] move(int move, int termWidth) {
        return move(move, termWidth, false);
    }

    protected char[] move(int move, int termWidth, boolean viMode) {
        move = moveCursor(move, viMode);

        //System.out.println("cursorWithPrompt:"+getCursorWithPrompt());
        //System.out.println("current row:"+(getCursorWithPrompt() / termWidth)+
        //        "new row:"+((move + getCursorWithPrompt()) / termWidth));
        //have we moved to another row?
        if(getCursorWithPrompt() / termWidth !=
                (move + getCursorWithPrompt()) / termWidth) {
            // if row is > 0, we need to move row number of rows down, oposite for row < 0
            int row = ((move + getCursorWithPrompt()) / termWidth) -
                    (getCursorWithPrompt() / termWidth) ;
            setCursor(getCursor() + move);
            if(row > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append(printAnsi(row+"B")).append(printAnsi(getCursorWithPrompt() % termWidth+"G"));
                return sb.toString().toCharArray();
            }
            //going up
            else {
                //check if we are on the "first" row:
                if(getCursor() <= termWidth) {
                }
                StringBuilder sb = new StringBuilder();
                sb.append(printAnsi(Math.abs(row)+"A")).append(printAnsi(getCursorWithPrompt() % termWidth+"G"));
                return sb.toString().toCharArray();
            }
        }

        //staying at the same row
        else {

            setCursor(getCursor() + move);

            if(move < 0)
                return printAnsi(Math.abs(move)+"D");

            else if(move > 0)
                return printAnsi(move+"C");
            else
                return new char[0];
        }
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

    /**
     * Return a ansified string based on param
     *
     * @param out what will be ansified
     * @return ansified string
     */
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
        for (char anOut : out) {
            if (anOut == '\t') {
                Arrays.fill(ansi, counter + 2, counter + 2 + TAB, ' ');
                counter += TAB - 1;
            } else
                ansi[counter + 2] = anOut;

            counter++;
        }

        return ansi;
    }

    /**
     * Make sure that the cursor do not move ob (out of bounds)
     *
     * @param move left if its negative, right if its positive
     * @param viMode if viMode we need other restrictions compared
     * to emacs movement
     * @return adjusted movement
     */
    private int moveCursor(final int move, boolean viMode) {
        // cant move to a negative value
        if(getCursor() == 0 && move <=0 )
            return 0;
        // cant move longer than the length of the line
        if(viMode) {
            if(getCursor() == line.length()-1 && (move > 0))
                return 0;
        }
        else {
            if(getCursor() == line.length() && (move > 0))
                return 0;
        }

        // dont move out of bounds
        if(viMode) {
            if(getCursor() + move > line.length()-1)
                return (line.length()-1-getCursor());
        }
        else {
            if(getCursor() + move > line.length())
                return (line.length()-getCursor());
        }

        if(getCursor() + move < 0)
            return -getCursor();

        return move;
    }

    /**
     * Get line from given param
     *
     * @param position in line
     * @return line from position
     */
    protected char[] getLineFrom(int position) {
        return line.substring(position).toCharArray();
    }

    public String getLine() {
        return line.toString();
    }

    protected void setLine(String line) {
        this.line = new StringBuilder(line);
    }

    protected void delete(int start, int end) {
        delta = end - start;
        line.delete(start, end);
    }

    protected void insert(int start, String in) {
        line.insert(start, in);
    }

    /**
     * Get the complete line (including prompt)
     *
     * @return complete line
     */
    public String getLineWithPrompt() {
        return prompt + line;
    }

    /**
     * Write a char to the line and update cursor accordingly
     *
     * @param c char
     */
    public void write(char c) {
        line.insert(cursor++, c);
        delta = 1;
    }

    /**
     * Write a string to the line and update cursor accordingly
     *
     * @param str string
     */
    public void write(final String str) {
        assert str != null;

        if (line.length() == 0) {
            line.append(str);
        }
        else {
            line.insert(cursor, str);
        }

        cursor += str.length();
        delta = str.length();
    }

    protected void clear() {
        line = new StringBuilder();
        delta = 0;
    }

    public int getDelta() {
        return delta;
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
                line.setCharAt(getCursor(), Character.toUpperCase(c));
            else
                line.setCharAt(getCursor(), Character.toLowerCase(c));

            return true;
        }
        else
            return false;
    }

    /**
     * Return the biggest common startsWith string
     *
     * @param completionList list to compare
     * @return biggest common startsWith string
     */
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
