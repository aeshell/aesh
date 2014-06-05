/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import java.util.Arrays;

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
    private boolean disablePrompt = false;
    private boolean multiLine = false;
    private StringBuilder multiLineBuffer;
    private static boolean ansi = true;

    private static final int TAB = 4;

    protected Buffer(boolean ansi) {
        this(ansi, null);
    }

    /**
     * Instantiate a Buffer with given prompt
     *
     * @param prompt set prompt
     */
    protected Buffer(boolean ansi, Prompt prompt ) {
        this.ansi = ansi;
        if(prompt != null)
            this.prompt = prompt;
        else
            this.prompt = new Prompt("");

        line = new StringBuilder();
        delta = 0;
    }

    protected Buffer(Prompt prompt) {
        this(true, prompt);
    }

    /**
     * Reset the buffer
     *
     * @param prompt set prompt
     */
    protected void reset(Prompt prompt) {
        if(prompt != null)
            this.prompt = prompt;
        else
            this.prompt = new Prompt("");
        cursor = 0;
        line = new StringBuilder();
        delta = 0;
        multiLine = false;
        multiLineBuffer = null;
    }

    /**
     * Reset the buffer, keep the existing prompt
     */
    protected void reset() {
        if(prompt == null)
            this.prompt = new Prompt("");
        cursor = 0;
        line = new StringBuilder();
        delta = 0;
        multiLine = false;
        multiLineBuffer = null;
    }

    protected void updatePrompt(Prompt prompt) {
        //only update if buffer contain items
        if(line.length() > 0) {
            this.prompt = prompt;
        }
        else
            reset(prompt);
    }

    /**
     * @return length of the line without prompt
     */
    protected int length() {
        if(prompt.isMasking() && prompt.getMask() == 0)
            return 1;
        else
            return line.length();
    }

    protected int totalLength() {
        if(prompt.isMasking()) {
            if(prompt.getMask() == 0)
                return disablePrompt ? 1 : getPrompt().getLength()+1;
        }
        return disablePrompt ? line.length()+1 : line.length() + getPrompt().getLength()+1;
    }

    protected int getCursor() {
        return (prompt.isMasking() && prompt.getMask() == 0) ? 0 : cursor;
    }

    protected int getCursorWithPrompt() {
        if(disablePrompt)
            return getCursor()+1;
        else
            return getCursor() + getPrompt().getLength()+1;
    }

    protected Prompt getPrompt() {
        if(!isMultiLine())
            return prompt;
        else
            return new Prompt("> ");
    }

    protected void setCursor(int cursor) {
        this.cursor = cursor ;
    }

    protected boolean isMultiLine() {
        return multiLine;
    }

    protected void setMultiLine(boolean m) {
        multiLine = m;
    }

    protected void updateMultiLineBuffer() {
        if(multiLineBuffer == null)
            multiLineBuffer = new StringBuilder();

        String newLine = line.toString();
        if(newLine.endsWith(" \\"))
            multiLineBuffer.append(newLine.substring(0, newLine.length()-1));
        else
            multiLineBuffer.append(newLine);
        line = new StringBuilder();
        cursor = 0;
    }

    protected String getMultiLineBuffer() {
        return multiLineBuffer.toString();
    }

    public boolean isMasking() {
        return prompt.isMasking();
    }

    /**
     * Need to disable prompt in calculations involving search.
     *
     * @param disable prompt or not
     */
    protected void disablePrompt(boolean disable) {
        disablePrompt = disable;
    }

    protected boolean isPromptDisabled() {
        return disablePrompt;
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
        if(!ansi)
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
        if(!prompt.isMasking())
            return line.toString();
        else {
            if(line.length() > 0 && prompt.getMask() != '\u0000')
                return String.format("%"+line.length()+"s", "").replace(' ', prompt.getMask());
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

    public String getMultiLine() {
        if (multiLine) {
            return getMultiLineBuffer() + getLine();
        } else {
            return getLine();
        }
    }

    public int getMultiCursor() {
        if (multiLine) {
            return multiLineBuffer.length() + getCursor();
        } else {
            return getCursor();
        }
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
        return getPrompt().getPromptAsString() + line;
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

    protected void replaceChar(char rChar, int pos) {
        if(pos > -1 && pos < line.length())
            line.setCharAt(pos, rChar);
    }

    protected boolean containRedirection() {
        return (line.indexOf(">") > -1 );
    }

    protected int getRedirectionPosition() {
       return line.indexOf(">");
    }
}
