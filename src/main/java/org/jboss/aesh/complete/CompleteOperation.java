/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.complete;

import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.terminal.TerminalString;

import java.util.ArrayList;
import java.util.List;

/**
 * A payload object to store completion data
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class CompleteOperation {
    private String buffer;
    private int cursor;
    private int offset;
    private List<TerminalString> completionCandidates;
    private boolean trimmed = false;
    private boolean ignoreStartsWith = false;
    private String nonTrimmedBuffer;
    private AeshContext aeshContext;


    private char separator = ' ';
    private boolean appendSeparator = true;
    private boolean ignoreOffset = false;

    public CompleteOperation(AeshContext aeshContext, String buffer, int cursor) {
        this.aeshContext = aeshContext;
        setCursor(cursor);
        setSeparator(' ');
        doAppendSeparator(true);
        completionCandidates = new ArrayList<>();
        setBuffer(buffer);
    }

    public String getBuffer() {
        return buffer;
    }

    private void setBuffer(String buffer) {
        if(buffer != null && buffer.startsWith(" ")) {
            trimmed = true;
            this.buffer = Parser.trimInFront(buffer);
            nonTrimmedBuffer = buffer;
            setCursor(cursor - getTrimmedSize());
        }
        else
            this.buffer = buffer;
    }

    public boolean isTrimmed() {
        return trimmed;
    }

    public int getTrimmedSize() {
        return nonTrimmedBuffer.length() - buffer.length();
    }

    public String getNonTrimmedBuffer() {
        return nonTrimmedBuffer;
    }

    public int getCursor() {
        return cursor;
    }

    private void setCursor(int cursor) {
        if(cursor < 0)
            this.cursor = 0;
        else
            this.cursor = cursor;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setIgnoreOffset(boolean ignoreOffset) {
        this.ignoreOffset = ignoreOffset;
    }

    public boolean doIgnoreOffset() {
        return ignoreOffset;
    }

    public AeshContext getAeshContext() {
        return aeshContext;
    }

    /**
     * Get the separator character, by default its space
     *
     * @return separator
     */
    public char getSeparator() {
        return separator;
    }

    /**
     * By default the separator is one space char, but
     * it can be overridden here.
     *
     * @param separator separator
     */
    public void setSeparator(char separator) {
        this.separator = separator;
    }

    /**
     * Do this completion allow for appending a separator
     * after completion? By default this is true.
     *
     * @return appendSeparator
     */
    public boolean hasAppendSeparator() {
        return appendSeparator;
    }

    /**
     * Set if this CompletionOperation would allow an separator to
     * be appended. By default this is true.
     *
     * @param appendSeparator appendSeparator
     */
    public void doAppendSeparator(boolean appendSeparator) {
        this.appendSeparator = appendSeparator;
    }

    public List<TerminalString> getCompletionCandidates() {
        return completionCandidates;
    }

    public void setCompletionCandidates(List<String> completionCandidates) {
        addCompletionCandidates(completionCandidates);
    }

    public void setCompletionCandidatesTerminalString(List<TerminalString> completionCandidates) {
        this.completionCandidates = completionCandidates;
    }

    public void addCompletionCandidate(TerminalString completionCandidate) {
        this.completionCandidates.add(completionCandidate);
    }

    public void addCompletionCandidate(String completionCandidate) {
        addStringCandidate(completionCandidate);
    }

    public void addCompletionCandidates(List<String> completionCandidates) {
        addStringCandidates(completionCandidates);
    }

    public void addCompletionCandidatesTerminalString(List<TerminalString> completionCandidates) {
        this.completionCandidates.addAll(completionCandidates);
    }

     public void removeEscapedSpacesFromCompletionCandidates() {
        Parser.switchEscapedSpacesToSpacesInTerminalStringList(getCompletionCandidates());
    }

    private void addStringCandidate(String completionCandidate) {
        this.completionCandidates.add(new TerminalString(completionCandidate, true));
    }

    private void addStringCandidates(List<String> completionCandidates) {
        for(String s : completionCandidates)
            addStringCandidate(s);
    }

    public List<String> getFormattedCompletionCandidates() {
        List<String> fixedCandidates = new ArrayList<String>(completionCandidates.size());
        for(TerminalString c : completionCandidates) {
            if(!ignoreOffset && offset < cursor) {
                int pos = cursor - offset;
                if(c.getCharacters().length() >= pos)
                    fixedCandidates.add(c.getCharacters().substring(pos));
                else
                    fixedCandidates.add("");
            }
            else {
                fixedCandidates.add(c.getCharacters());
            }
        }
        return fixedCandidates;
    }

    public List<TerminalString> getFormattedCompletionCandidatesTerminalString() {
        List<TerminalString> fixedCandidates = new ArrayList<TerminalString>(completionCandidates.size());
        for(TerminalString c : completionCandidates) {
            if(!ignoreOffset && offset < cursor) {
                int pos = cursor - offset;
                if(c.getCharacters().length() >= pos) {
                    TerminalString ts = c;
                    ts.setCharacters(c.getCharacters().substring(pos));
                    fixedCandidates.add(ts);
                }
                else
                    fixedCandidates.add(new TerminalString("", true));
            }
            else {
                fixedCandidates.add(c);
            }
        }
        return fixedCandidates;
    }

    public String getFormattedCompletion(String completion) {
        if(offset < cursor) {
            int pos = cursor - offset;
            if(completion.length() > pos)
                return completion.substring(pos);
            else
                return "";
        }
        else
            return completion;
    }

    public boolean isIgnoreStartsWith() {
        return ignoreStartsWith;
    }

    public void setIgnoreStartsWith(boolean ignoreStartsWith) {
        this.ignoreStartsWith = ignoreStartsWith;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Buffer: ").append(buffer)
                .append(", Cursor:").append(cursor)
                .append(", Offset:").append(offset)
                .append(", IgnoreOffset:").append(ignoreOffset)
                .append(", Append separator: ").append(appendSeparator)
                .append(", Candidates:").append(completionCandidates);

        return sb.toString();
    }

}
