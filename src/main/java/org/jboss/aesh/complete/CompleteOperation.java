/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.complete;

import org.jboss.aesh.util.Parser;

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
    private List<String> completionCandidates;
    private boolean trimmed = false;
    private int trimmedSize = 0;
    private String nonTrimmedBuffer;


    private char separator = ' ';
    private boolean appendSeparator = true;

    public CompleteOperation(String buffer, int cursor) {
        setCursor(cursor);
        setSeparator(' ');
        doAppendSeparator(true);
        completionCandidates = new ArrayList<String>();
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

    public List<String> getCompletionCandidates() {
        return completionCandidates;
    }

    public void setCompletionCandidates(List<String> completionCandidates) {
        this.completionCandidates = completionCandidates;
    }

    public void addCompletionCandidate(String completionCandidate) {
        this.completionCandidates.add(completionCandidate);
    }

    public void addCompletionCandidates(List<String> completionCandidates) {
        this.completionCandidates.addAll(completionCandidates);
    }

    public void removeEscapedSpacesFromCompletionCandidates() {
        setCompletionCandidates(Parser.switchEscapedSpacesToSpacesInList(getCompletionCandidates()));
    }

    public List<String> getFormattedCompletionCandidates() {
        if(offset < cursor) {
            List<String> fixedCandidates = new ArrayList<String>(completionCandidates.size());
            int pos = cursor - offset;
            for(String c : completionCandidates) {
                if(c.length() >= pos)
                    fixedCandidates.add(c.substring(pos));
                else
                    fixedCandidates.add("");
            }
            return fixedCandidates;
        }
        else
            return completionCandidates;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Buffer: ").append(buffer).append(", Cursor:").append(cursor).append(", Offset:").append(offset);
        sb.append(", candidates:").append(completionCandidates);

        return sb.toString();
    }

}
