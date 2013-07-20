/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.settings.Settings;

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
    private Prompt prompt;
    private int delta; //need to keep track of a delta for ansi terminal
    private Character mask;
    private boolean disablePrompt = false;

    private static final int TAB = 4;

    protected Buffer() {
        this(null, null);
    }

    /**
     * Instantiate a Buffer with given prompt
     *
     * @param prompt set prompt
     */
    protected Buffer(Prompt prompt) {
        this(prompt, null);
    }

    protected Buffer(Prompt prompt, Character mask) {
        if(prompt != null)
            this.prompt = prompt;
        else
            this.prompt = new Prompt("");

        line = new StringBuilder();
        delta = 0;
        this.mask = mask;
    }

    /**
     * Reset the buffer
     *
     * @param prompt set prompt
     */
    protected void reset(Prompt prompt) {
        reset(prompt, null);
    }

    protected void reset(Prompt prompt, Character mask) {
        if(prompt != null)
            this.prompt = prompt;
        else
            this.prompt = new Prompt("");
        cursor = 0;
        line = new StringBuilder();
        delta = 0;
        this.mask = mask;
    }

    /**
     * @return length of the line without prompt
     */
    protected int length() {
        if(mask != null && mask == 0)
            return 1;
        else
            return line.length();
    }

    protected int totalLength() {
        if(mask != null) {
            if(mask == 0)
                return disablePrompt ? 1 : prompt.getLength()+1;
        }
        return disablePrompt ? line.length()+1 : line.length() + prompt.getLength()+1;
    }

    protected int getCursor() {
        return (mask != null && mask == 0) ? 0 : cursor;
    }

    protected int getCursorWithPrompt() {
        if(disablePrompt)
            return getCursor()+1;
        else
            return getCursor() + prompt.getLength()+1;
    }

    protected Prompt getPrompt() {
        return prompt;
    }

    protected void setCursor(int cursor) {
        this.cursor = cursor ;
    }

    public boolean isMasking() {
        return mask != null;
    }

    /**
     * Need to disable prompt in calculations involving search.
     *
     * @param disable prompt or not
     */
    protected void disablePrompt(boolean disable) {
        disablePrompt = disable;
    }

    /**
     * Move the cursor left if the param is negative,
     * and right if its positive.
     * Return ansi code to represent the move
     *
     * @param move where to move
     * @param termWidth terminal width
     * @return ansi string that represent the move
     */
    protected char[] move(int move, int termWidth) {
        return move(move, termWidth, false);
    }

    protected char[] move(int move, int termWidth, boolean viMode) {
        move = moveCursor(move, viMode);

        int currentRow = (getCursorWithPrompt() / (termWidth));
        if(currentRow > 0 && getCursorWithPrompt() % termWidth == 0)
            currentRow--;

        int newRow = ((move + getCursorWithPrompt()) / (termWidth));
        if(newRow > 0 && ((move + getCursorWithPrompt()) % (termWidth) == 0))
            newRow--;

        int row = newRow - currentRow;

        setCursor(getCursor() + move);
        int cursor = getCursorWithPrompt() % termWidth;
        if(cursor == 0 && getCursorWithPrompt() > 0)
            cursor = termWidth;
        if(row > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(printAnsi(row+"B")).append(printAnsi(cursor+"G"));
            return sb.toString().toCharArray();
        }
        //going up
        else if (row < 0) {
            //check if we are on the "first" row:
            if(getCursor() <= termWidth) {
            }
            StringBuilder sb = new StringBuilder();
            sb.append(printAnsi(Math.abs(row)+"A")).append(printAnsi(cursor+"G"));
            return sb.toString().toCharArray();
        }
        //staying at the same row
        else {
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
        if(!Settings.getInstance().isAnsiConsole())
            return new char[] {};
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
            if(getCursor() == length()-1 && (move > 0))
                return 0;
        }
        else {
            if(getCursor() == length() && (move > 0))
                return 0;
        }

        // dont move out of bounds
        if(getCursor() + move <= 0)
            return -getCursor();

        if(viMode) {
            if(getCursor() + move > length()-1)
                return (length()-1-getCursor());
        }
        else {
            if(getCursor() + move > length())
                return (length()-getCursor());
        }

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
        if(mask == null)
            return line.toString();
        else {
            if(line.length() > 0)
                return String.format("%"+line.length()+"s", "").replace(' ', mask);
            else
                return "";
        }
    }

    public String getLineNoMask() {
        return line.toString();
    }

    protected void setLine(String line) {
        delta = line.length() - this.line.length();
        this.line = new StringBuilder(line);
    }

    protected void delete(int start, int end) {
        delta = start - end;
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
        return prompt.getPromptAsString() + line;
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
            line.insert(getCursor(), str);
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

    protected void replaceChar(char rChar) {
        line.setCharAt(getCursor(), rChar);
    }

    protected boolean containRedirection() {
        return (line.indexOf(">") > -1 );
    }

    protected int getRedirectionPosition() {
       return line.indexOf(">");
    }
}
